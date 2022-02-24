clean_assemble {
  tasks = ["clean", ":app:assembleDebug"]
}

clean_assemble_no_cache {
  tasks = ["clean", ":app:assembleDebug"]
  gradle-args = ["--no-build-cache", "--no-configuration-cache"]
}
