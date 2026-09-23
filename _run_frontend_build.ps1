# 前端构建执行（便携工具链）— MDM-0001 测试完成阶段证据
$tools = 'C:\Users\wenbchen\Desktop\Work\_tools'
$repo  = 'c:\Users\wenbchen\git\dev-harness-framework'

$env:Path = "$tools\node;" + $env:Path
$env:npm_config_cache = "$repo\.cache\npm"

Set-Location "$repo\mdm-frontend"
Write-Host '=== npm install start ==='
& "$tools\node\npm.cmd" install --no-audit --no-fund
Write-Host ("NPM_INSTALL_EXIT=" + $LASTEXITCODE)
Write-Host '=== npm run build start ==='
& "$tools\node\npm.cmd" run build
Write-Host ("NPM_BUILD_EXIT=" + $LASTEXITCODE)
