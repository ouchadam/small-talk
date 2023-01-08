import * as google from '@googleapis/androidpublisher'
import * as fs from "fs"
import * as http from 'https'
import * as matrix from 'matrix-js-sdk'
import * as url from 'url'

const __dirname = url.fileURLToPath(new URL('.', import.meta.url));

const auth = new google.auth.GoogleAuth({
    keyFile: '.secrets/service-account.json',
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
})

const androidPublisher = google.androidpublisher({
    version: 'v3',
    auth: auth,
})

const universalApkPath = `${__dirname}/universal.apk`

export const release = async (github, version, applicationId, artifacts, config) => {
    const appEditId = await startPlayRelease(applicationId)

    console.log("Uploading bundle...")
    await uploadBundle(appEditId, applicationId, artifacts.bundle)

    console.log("Uploading mapping...")
    await uploadMappingFile(appEditId, version.code, applicationId, artifacts.mapping)

    console.log("Assign artifacts to beta release...")
    await addReleaseToTrack(appEditId, version, applicationId)

    console.log("Commiting draft release...")
    await androidPublisher.edits.commit({
        editId: appEditId,
        packageName: applicationId,
    }).catch((error) => Promise.reject(error.response.data))

    console.log("Downloading generated universal apk...")
    await dowloadSignedUniversalApk(
        version,
        applicationId,
        await auth.getAccessToken(),
        universalApkPath
    )

    const releaseResult = await github.rest.repos.createRelease({
        owner: config.owner,
        repo: config.repo,
        tag_name: version.name,
        prerelease: false,
        generate_release_notes: true,
    })

    console.log(releaseResult.data.id)

    console.log("Uploading universal apk...")
    await github.rest.repos.uploadReleaseAsset({
        owner: config.owner,
        repo: config.repo,
        release_id: releaseResult.data.id,
        name: `universal-${version.name}.apk`,
        data: fs.readFileSync(universalApkPath)
    })

    console.log("Uploading foss apk...")
    await github.rest.repos.uploadReleaseAsset({
        owner: config.owner,
        repo: config.repo,
        release_id: releaseResult.data.id,
        name: `foss-signed-${version.name}.apk`,
        data: fs.readFileSync(artifacts.fossApkPath)
    })

    console.log("Promoting beta draft release to live...")
    await promoteDraftToLive(applicationId)

    console.log("Sending message to room...")
    await sendReleaseMessage(releaseResult.data, config)
}

const startPlayRelease = async (applicationId) => {
    const result = await androidPublisher.edits.insert({
        packageName: applicationId
    }).catch((error) => Promise.reject(error.response.data))
    return result.data.id
}

const uploadBundle = async (appEditId, applicationId, bundleReleaseFile) => {
    const res = await androidPublisher.edits.bundles.upload({
        packageName: applicationId,
        editId: appEditId,
        media: {
            mimeType: 'application/octet-stream',
            body: fs.createReadStream(bundleReleaseFile)
        }
    }).catch((error) => Promise.reject(error.response.data))

    return res.data
}

const uploadMappingFile = async (appEditId, versionCode, applicationId, mappingFilePath) => {
    await androidPublisher.edits.deobfuscationfiles.upload({
        packageName: applicationId,
        editId: appEditId,
        apkVersionCode: versionCode,
        deobfuscationFileType: 'proguard',
        media: {
            mimeType: 'application/octet-stream',
            body: fs.createReadStream(mappingFilePath)
        }
    }).catch((error) => Promise.reject(error.response.data))
}

const addReleaseToTrack = async (appEditId, version, applicationId) => {
    const result = await androidPublisher.edits.tracks
        .update({
            editId: appEditId,
            packageName: applicationId,
            track: "beta",
            requestBody: {
                track: "beta",
                releases: [
                    {
                        name: version.name,
                        status: "draft",
                        releaseNotes: {
                            language: "en-GB",
                            text: "Bug fixes and improvments - See https://github.com/ouchadam/small-talk/releases for more details",
                        },
                        versionCodes: [version.code]
                    }
                ]
            }
        })
        .catch((error) => Promise.reject(error.response.data))
    return result.data;
}


const dowloadSignedUniversalApk = async (version, applicationId, authToken, outputFile) => {
    console.log("fetching universal apk")

    const apkRes = await androidPublisher.generatedapks.list({
        packageName: applicationId,
        versionCode: version.code,
    })

    const apks = apkRes.data.generatedApks
    const id = apks[0].generatedUniversalApk.downloadId

    console.log(`downloading: ${id}`)

    const downloadUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${applicationId}/generatedApks/${version.code}/downloads/${id}:download?alt=media`
    const options = {
        headers: {
            "Authorization": `Bearer ${authToken}`
        }
    }

    await downloadToFile(downloadUrl, options, outputFile)
}

const downloadToFile = async (url, options, outputFile) => {
    return new Promise((resolve, error) => {
        http.get(url, options, (response) => {
            const file = fs.createWriteStream(outputFile)
            response.pipe(file)

            file.on("finish", () => {
                file.close()
                resolve()
            })

            file.on("error", (cause) => {
                error(cause)
            })
        }).on("error", (cause) => {
            error(cause)
        })
    })
}

const promoteDraftToLive = async (applicationId) => {
    const editId = await startPlayRelease(applicationId)

    await androidPublisher.edits.tracks
        .update({
            editId: editId,
            packageName: applicationId,
            track: "beta",
            requestBody: {
                track: "beta",
                releases: [
                    {
                        status: "completed",
                    }
                ]
            }
        })
        .catch((error) => Promise.reject(error.response.data))


    await androidPublisher.edits.commit({
        editId: editId,
        packageName: applicationId,
    }).catch((error) => Promise.reject(error.response.data))
}

const sendReleaseMessage = async (release, config) => {
    const matrixAuth = JSON.parse(fs.readFileSync('.secrets/matrix.json'))
    const client = matrix.createClient(matrixAuth)
    const content = {
        "body": `New release`,
        "format": "org.matrix.custom.html",
        "formatted_body": `New release rolling out <a href="${release.html_url}">${release.tag_name}</a>`,
        "msgtype": "m.text"
    }
    await client.sendEvent(config.matrixRoomId, "m.room.message", content, "")
}
