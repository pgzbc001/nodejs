# 后端测试执行（便携工具链）— MDM-0001 测试完成阶段证据
$tools = 'C:\Users\wenbchen\Desktop\Work\_tools'
$repo  = 'c:\Users\wenbchen\git\dev-harness-framework'

$env:JAVA_HOME = "$tools\jdk17"
$env:Path = "$tools\jdk17\bin;$tools\maven\bin;" + $env:Path
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Djava.io.tmpdir=$repo\.cache\tmp -XX:-UsePerfData"

New-Item -ItemType Directory -Force "$repo\.cache\tmp" | Out-Null
New-Item -ItemType Directory -Force "$repo\.cache\m2repo" | Out-Null

$m2repo = ($repo -replace '\\', '/') + '/.cache/m2repo'

Set-Location "$repo\mdm-backend"
Write-Host '=== mvn test start ==='
& "$tools\maven\bin\mvn.cmd" -B "-Dmaven.repo.local=$m2repo" test
Write-Host ("MVN_EXIT=" + $LASTEXITCODE)
