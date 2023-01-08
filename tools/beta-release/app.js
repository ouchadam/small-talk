import { release } from './release.js'

const config = {
    owner: "ouchadam",
    repo: "small-talk",
    pathToVersionFile: "version.json",
    rcBranchesFrom: "main",
    rcMergesTo: "release",
    packageName: "app.dapk.st",
    matrixRoomId: "!fuHEgUsoPRBQynkdkF:iswell.cool"
}

const rcBranchName = "release-candidate"

export const startReleaseProcess = async ({ github, context, core }) => {
    console.log("script start")
    if (await doesNotHaveInProgressRelease(github) && await isWorkingBranchAhead(github)) {
        await startRelease(github)
    } else {
        console.log(`Release skipped due to being behind`)
    }
    return ""
}

export const publishRelease = async (github, artifacts) => {
    const versionFile = await readVersionFile(github, config.rcMergesTo)
    await release(
        github,
        versionFile.content,
        config.packageName,
        artifacts,
        config,
    )

    const createdPr = await github.rest.pulls.create({
        owner: config.owner,
        repo: config.repo,
        title: "[Auto] Sync Release",
        head: config.rcMergesTo,
        base: config.rcBranchesFrom,
        body: "Syncing changes from release",
    })

    await enablePrAutoMerge(github, createdPr.data.node_id)
}

const isWorkingBranchAhead = async (github) => {
    const result = await github.rest.repos.compareCommitsWithBasehead({
        owner: config.owner,
        repo: config.repo,
        basehead: `${config.rcMergesTo}...${config.rcBranchesFrom}`,
        per_page: 1,
        page: 1,
    })
    return result.data.status === "ahead"
}

const doesNotHaveInProgressRelease = async (github) => {
    const releasePrs = await github.rest.pulls.list({
        owner: config.owner,
        repo: config.repo,
        state: "open",
        base: config.rcMergesTo
    })

    const syncPrs = await github.rest.pulls.list({
        owner: config.owner,
        repo: config.repo,
        state: "open",
        base: config.rcBranchesFrom,
        head: `${config.owner}:${config.rcMergesTo}`
    })

    return releasePrs.data.length === 0 && syncPrs.data.length === 0
}

const startRelease = async (github) => {
    console.log(`creating release candidate from head of ${config.rcBranchesFrom}`)

    await createBranch(github, "release-candidate", config.rcBranchesFrom)
    await incrementVersionFile(github, rcBranchName)

    const createdPr = await github.rest.pulls.create({
        owner: config.owner,
        repo: config.repo,
        title: "[Auto] Release Candidate",
        head: rcBranchName,
        base: config.rcMergesTo,
        body: "todo",
    })

    await enablePrAutoMerge(github, createdPr.data.node_id)
}

const enablePrAutoMerge = async (github, prNodeId) => {
    await github.graphql(
        `
        mutation ($pullRequestId: ID!, $mergeMethod: PullRequestMergeMethod!) {
            enablePullRequestAutoMerge(input: {
              pullRequestId: $pullRequestId,
              mergeMethod: $mergeMethod
            }) {
              pullRequest {
                autoMergeRequest {
                  enabledAt
                  enabledBy {
                    login
                  }
                }
              }
            }
          }
        `,
        {
            pullRequestId: prNodeId,
            mergeMethod: "MERGE"
        }
    )
}

const createBranch = async (github, branchName, fromBranch) => {
    const mainRef = await github.rest.git.getRef({
        owner: config.owner,
        repo: config.repo,
        ref: `heads/${fromBranch}`,
    })

    await github.rest.git.createRef({
        owner: config.owner,
        repo: config.repo,
        ref: `refs/heads/${branchName}`,
        sha: mainRef.data.object.sha,
    })
}

const incrementVersionFile = async (github, branchName) => {
    const versionFile = await readVersionFile(github, branchName)
    const [date, rc] = versionFile.content.name.split(".1")
    const today = new Date()
    const month = (today.getMonth() + 1).toString().padStart(2, '0')
    const day = (today.getDay() + 1).toString().padStart(2, '0')
    const year = today.getFullYear().toString().slice(-2)
    const todayFormatted = `${year}/${month}/${day}`

    let updatedVersionName = undefined
    if (todayFormatted == date) {
        updatedVersionName = `${date}.${parseInt(rc) + 1}`
    } else {
        updatedVersionName = `${todayFormatted}.1`
    }

    const updatedVersionFile = {
        code: versionFile.content.code + 1,
        name: updatedVersionName,
    }

    const encodedContentUpdate = Buffer.from(JSON.stringify(updatedVersionFile, null, 2)).toString('base64')
    await github.rest.repos.createOrUpdateFileContents({
        owner: config.owner,
        repo: config.repo,
        content: encodedContentUpdate,
        path: config.pathToVersionFile,
        sha: versionFile.sha,
        branch: branchName,
        message: "updating version for release",
    })
}

const readVersionFile = async (github, branch) => {
    const result = await github.rest.repos.getContent({
        owner: config.owner,
        repo: config.repo,
        path: config.pathToVersionFile,
        ref: branch,
    })

    const content = Buffer.from(result.data.content, result.data.encoding).toString()
    return {
        content: JSON.parse(content),
        sha: result.data.sha,
    }
}
