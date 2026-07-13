# batch-pack-skills.ps1
# 将 aik-skills-lab 下每个技能文件夹打包为独立的 .zip 文件
# 使用正斜杠路径，兼容 Linux unzip
# 用法: .\batch-pack-skills.ps1

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$SkillsRoot = "d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab"
$OutputDir  = "d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab\_upload-ready"

# 清理并创建输出目录
if (Test-Path $OutputDir) { Remove-Item $OutputDir -Recurse -Force }
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

$count = 0

$skillDirs = Get-ChildItem -Path $SkillsRoot -Directory | Where-Object {
    $_.Name -ne "_upload-ready" -and (Test-Path (Join-Path $_.FullName "SKILL.md"))
}

foreach ($dir in $skillDirs) {
    $skillName = $dir.Name
    $skillPath = $dir.FullName
    $zipPath   = Join-Path $OutputDir "$skillName.zip"

    # 创建 zip（使用正斜杠路径）
    $zip = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)

    $files = Get-ChildItem -Path $skillPath -Recurse -File
    foreach ($file in $files) {
        # 计算相对路径并转换为正斜杠
        $relativePath = $file.FullName.Substring($skillPath.Length + 1)
        $entryName = $relativePath.Replace('\', '/')

        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip,
            $file.FullName,
            $entryName,
            [System.IO.Compression.CompressionLevel]::Optimal
        ) | Out-Null
    }

    $zip.Dispose()

    $count++
    $sizeKB = [math]::Round((Get-Item $zipPath).Length / 1KB, 1)
    Write-Host "[$count] $skillName.zip ($sizeKB KB)" -ForegroundColor Green
}

Write-Host ""
Write-Host "===== Done: $count skills packed to $OutputDir =====" -ForegroundColor Cyan
