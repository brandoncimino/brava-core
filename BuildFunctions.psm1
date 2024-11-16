<# 
    This module contains functions to help you build and publish the project to Maven Central using JReleaser.
    
    Note that these are meant to be MANUALLY EXECUTED to help you set things up, and NOT a part of a full-on automated workflow.
#>

#region Dependencies

function Get-CommandPath(
    [Parameter(Mandatory)]
    [ValidateNotNullOrWhiteSpace()]
    [string]$Name,

    [switch]$UseWsl
) {
    $PSNativeCommandUseErrorActionPreference = $true
    $ErrorActionPreference = 'Stop'

    if ($UseWsl) {
        if(!$IsWindows){
            throw "You can't '-UseWsl' on the operating system $($PSVersionTable.OS)."
        }

        Write-Verbose "Checking for the command '$Name' in WSL"
        return wsl --shell-type login -- pwsh -Command "(Get-Command -Name '$Name' -ErrorAction SilentlyContinue).Source"
    }
    else {
        return (Get-Command -Name $Name -ErrorAction SilentlyContinue).Source
    }
}

function Install-Command(
    [Parameter(Mandatory)]
    [ValidateNotNullOrWhiteSpace()]
    [string]$Name,
    [scriptblock]$WindowsScript,
    [scriptblock]$MacScript,
    [scriptblock]$LinuxScript,
    [switch]$UseWsl,
    [switch]$Force
) {
    $PSNativeCommandUseErrorActionPreference = $true
    $ErrorActionPreference = 'Stop'

    if($UseWsl){
        if(!$IsWindows){
            throw "You can't '-UseWsl' on the operating system $($PSVersionTable.OS)."
        }
    }

    $pretty_command = "$($PSStyle.Formatting.FormatAccent)$Name$($PSStyle.Reset)"

    $already_installed = Get-CommandPath -Name $Name -UseWsl:$UseWsl

    if ($already_installed) {
        if ($Force) {
            Write-Host "🪠 The command '$pretty_command' was already installed at $($PSStyle.Formatting.FormatAccent)$($already_installed.Path), but he '-Force' parameter was $Force, so we are going to install it anyways."
        }
        else {
            Write-Host "⏭️ The command '$pretty_command' is already installed at $($PSStyle.Formatting.FormatAccent)$($already_installed.Path)$($PSStyle.Reset)."
            return
        }
    }

    Write-Host "📥 Installing the command '$pretty_command'."

    if($UseWsl){
        if(!$LinuxScript){
            throw "You have requested to '-UseWsl', so you must provide a '-LinuxScript'."
        }

        wsl pwsh $LinuxScript
    }
    elseif ($IsWindows) {
        if(!$WindowsScript){
            throw "You are using Windows, so you must provide a '-WindowsScript'."
        }

        &$WindowsScript
    }
    elseif($IsMacOS){
        if(!$MacScript){
            throw "You are on Mac, so you must provide a '-MacScript'."
        }

        &$MacScript
    }
    elseif($IsLinux){
        if(!$LinuxScript){
            throw "You are on Linux, so you must provide a '-LinuxScript'."
        }

        &$LinuxScript
    }
    else {
        throw "🙅‍♀️ $($PSStyle.FormatHyperlink("Palm OS", "https://en.wikipedia.org/wiki/Palm_OS") ) is not supported!"
    }

    $success = Get-CommandPath $Name -UseWsl:$UseWsl

    if($success){
        Write-Host "✅ Installed '$pretty_command' to $($PSStyle.Formatting.FormatAccent)$success$($PSStyle.Reset)."
    }
    else {
        throw "❌ Wasn't able to install '$pretty_command'!"
    }
}

function Use-Chocolatey(){
    if(!$IsWindows){
        throw "Chocolatey is only for Windows."
    }

    Install-Command -Name 'choco' -WindowsScript {
        Write-Host "Installing Chocolatey with the official installation script at https://chocolatey.org/install"
        Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    }
}

function Use-Gpg(){
    $splat = @{
        Name = 'gpg'
        WindowsScript = {
            Use-Chocolatey
            choco install gpg4win -a
        }
        MacScript = {
            throw "I dunno, I don't have a mac 🤷‍♀️"
        }
        LinuxScript = {
            throw "I though this was suppose to come on Linux automatically?"
        }
    }
    Install-Command @splat
}

<# 
.SYNOPSIS
    Installs the Github command line tool, `gh`, and authenticates with it.
#>
function Use-Github() {
    $PSNativeCommandUseErrorActionPreference = $true
    $ErrorActionPreference = 'Stop'

    $splat = @{
        Name = 'gh'
        WindowsScript = {
            Use-Chocolatey
            choco install gh
        }
        MacScript = {
            brew install gh
        }
        LinuxScript = {
            # Taken from: https://github.com/cli/cli/blob/trunk/docs/install_linux.md
            bash -c @'
type -p curl >/dev/null || (sudo apt update && sudo apt install curl -y)
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg \
&& sudo chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg \
&& echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null \
&& sudo apt update \
&& sudo apt install gh -y
'@
        }
    }
    Install-Command @splat
    
    # 📎 Don't redirect the output from `gh` commands into variables or `Write-{X}` or anything like that - we want to preserve the fancy `gh` tool's magic.
    try {
        # If you aren't logged in, `gh auth status` returns a non-0 exit code, which will cause an exception to be thrown because `$PSNativeCommandUseErrorActionPreference = $true`.
        gh auth status
    }
    catch {
        gh auth login --hostname github.com --git-protocol https
    }
}

#endregion


<#
.SYNOPSIS
    Retrieves the *first* "gpg key id" from the output of `gpg --list-keys`.

.NOTES
    For use with other commands like `gpg --export`.
#>
function Get-GpgKeyId() {
    $PSNativeCommandUseErrorActionPreference = $true
    $ErrorActionPreference = 'Stop'

    Use-Gpg

    [string[]] $listKeysOutput = gpg --list-keys --armor

    for ($i = 0; $i -lt $listKeysOutput.Count; $i++) {
        if ( $listKeysOutput[$i].StartsWith("pub")) {
            return $listKeysOutput[$i + 1].Trim()
        }
    }

    throw "Couldn't find a GPG key ID from the output of 'gpg --list-keys' 😟"
}

<#
.SYNOPSIS
    Retrieves the content of your "gpg public key", used for `JRELEASER_GPG_PUBLIC_KEY`.
#>
function Get-GpgPublicKey(
    $GpgKeyId = (Get-GpgKeyId)
) {
    $PSNativeCommandUseErrorActionPreference = $true
    $ErrorActionPreference = 'Stop'

    Use-Gpg

    $gpgPublicContent = gpg --armor --export $GpgKeyId | Join-String -Separator "`n"
    [ArgumentNullException]::ThrowIfNullOrEmpty($gpgPublicContent, "gpgPublicContent")
    return $gpgPublicContent
}

<#
.SYNOPSIS
    Retrieves the content of your "gpg secret key", used for `JRELEASER_GPG_SECRET_KEY`.
#>
function Get-GpgSecretKey(
    $GpgKeyId = (Get-GpgKeyId)
) {
    $PSNativeCommandUseErrorActionPreference = $true
    $ErrorActionPreference = 'Stop'

    Use-Gpg

    # This command will open a popup application window, where you will be required to enter your "passphrase" (💬)
    $gpgSecretContent = gpg --armor --export-secret-keys $gpgKeyId | Join-String -Separator "`n"

    [ArgumentNullException]::ThrowIfNullOrEmpty($gpgSecretContent, "gpgSecretContent")
    return $gpgSecretContent
}

<# 
.SYNOPSIS
    Outputs the configurations that JReleaser is currently able to find.
#>
function Get-JReleaserConfigs(
    [Parameter(Mandatory)]
    [securestring]$GpgPassphrase,
    [string]$GpgPublicKey = (Get-GpgPublicKey),
    [string]$GpgSecretKey = (Get-GpgSecretKey),
    [string]$GithubUser = (Get-GithubUser),
    [string]$GithubToken = (gh auth token)
) {
    $passphrase_raw = $GpgPassphrase | ConvertFrom-SecureString -AsPlainText
    return @{
        JRELEASER_GPG_PASSPHRASE = $passphrase_raw
        JRELEASER_GPG_PUBLIC_KEY = $GpgPublicKey
        JRELEASER_GPG_SECRET_KEY = $GpgSecretKey
        # TODO: Find out if these `MAVENCENTRAL` configurations are actually used 
        # JRELEASER_MAVENCENTRAL_USERNAME = /* from maven central website, and then I had to save them */
        # JRELEASER_MAVENCENTRAL_TOKEN = /* from maven central website, and then I had to save them */
        JRELEASER_GITHUB_TOKEN   = $GithubToken
    }
}

<#
.SYNOPSIS
    Adds or updates some key-value pairs somewhere.
#>
function Export-Configs(
    [hashtable]$Configs,
    [switch]$GithubSecrets,
    [ValidateNotNullOrWhitespace()]
    [string]$File,
    [ValidateSet("yaml", "json")]
    [string]$FileType,
    [switch]$EnvironmentVariables,
    [switch]$Force
) {
    if ($GithubSecrets) {
        Write-Verbose "Exporting $($configs.Keys -join ", ") to your Github secrets."
        foreach ($key in $Configs.Keys) {
            Use-Github
            gh secret set $key --body $Configs[$key]
        }
    }

    if ($File) {
        Write-Verbose "Exporting $($configs.Keys -join ", ") to the file $File."
        if (!$FileType) {
            [regex]$yaml_pat = "\.ya?ml"
            $FileType = switch ([System.IO.Path]::GetExtension($File)) {
                $yaml_pat { ".yaml" }
                ".json" { ".json" }
                default { throw "Unknown type for the '-File' $File. Please explicitly set the '-FileType' parameter." }
            }
        }

        $existing_config_file = Get-Item $File -ErrorAction SilentlyContinue

        $existing_configs = if ($existing_config_file) {
            switch ($FileType) {
                ".yaml" { $existing_config_file | Get-Content | ConvertFrom-Yaml }
                ".json" { $existing_config_file | Get-Content | ConvertFrom-Json -AsHashtable }
                default { throw "Unknown file type: $FileType" }
            }
        }

        Write-Verbose "Got $($existing_configs.Count) existing configs:`n$($existing_configs | Out-String)"

        foreach ($key in $Configs.Keys) {
            $existing_configs[$key] = $Configs[$key]
        }

        switch ($FileType) {
            ".yaml" { $existing_configs | ConvertTo-Yaml -OutFile $config_file }
            ".json" { $existing_configs | ConvertTo-Json > $config_file }
            default { throw "Unknown file type: $FileType" }
        }
    }

    if ($EnvironmentVariables) {
        Write-Verbose "Exporting $($Configs.Keys -join ", ") to your environment variables."
        foreach ($key in $Configs.Keys) {
            Set-Item -Path "env:${key}" -Value $Configs[$key]
        }
    }
}

<#
.SYNOPSIS
    Retrieves the *NAMES* of the configurations that JReleaser finds via [jreleaserEnv](https://jreleaser.org/guide/latest/tools/jreleaser-gradle.html#_jreleaserenv)
#>
function Get-CurrentJReleaserConfigs(
    $GradleCommand = (Get-Command .\gradlew)
) {
    $PSNativeCommandUseErrorActionPreference = $true
    $ErrorActionPreference = 'Stop'

    &$GradleCommand jreleaserEnv --stacktrace | 
    Where-Object { $_ -cmatch "JRELEASER" } | 
    ForEach-Object { $_ -replace '\[.*]\s*', '' } | 
    Write-Output
}



function Test-JReleaserConfig(
    
) {
    $PSNativeCommandUseErrorActionPreference = $true
    $ErrorActionPreference = 'Stop'
    
    [string[]]$current_configs = Get-CurrentJReleaserConfigs

    $mavencentral_recourse = @"
$($PSStyle.Formatting.Warning)1. Log in to https://central.sonatype.com/account.
  2. Click on "Generate User Token."
  3. Save the output somewhere for future reference, like in a password manager.
  4. Set $($PSStyle.Formatting.FormatAccent)JRELEASER_MAVENCENTRAL_USERNAME$($PSStyle.Formatting.Warning) to the value of the '<username>' node.
  5. Set $($PSStyle.Formatting.FormatAccent)JRELEASER_MAVENCENTRAL_TOKEN$($PSStyle.Formatting.Warning) to the value of the '<password>' node.$($PSStyle.Reset)
"@

    $configs_and_remediation = @{
        JRELEASER_GITHUB_TOKEN          = { gh auth token }
        JRELEASER_GPG_PUBLIC_KEY        = { Get-GpgPublicKey }
        JRELEASER_GPG_SECRET_KEY        = { Get-GpgSecretKey }
        JRELEASER_GPG_PASSPHRASE        = { 
            Read-Host -AsSecureString -Prompt "Enter the GPG passphrase for your secret key." | ConvertFrom-SecureString -AsPlainText
        }
        JRELEASER_MAVENCENTRAL_USERNAME = { Write-Warning $mavencentral_recourse }
        JRELEASER_MAVENCENTRAL_TOKEN    = { Write-Warning $mavencentral_recourse }
    }

    $unrecoverable = @()

    foreach ($key in $configs_and_remediation.Keys) {
        if ($current_configs -contains $key) {
            Write-Host "✅ $($PSStyle.Formatting.FormatAccent)$key$($PSStyle.Reset) is available."
        }
        else {
            Write-Host "❌ $($PSStyle.Formatting.Error)$key$($PSStyle.Reset) is missing!"

            $remediated_value = &$configs_and_remediation[$key]
            if ($remediated_value) {
                Write-Host "🩹 Recovered a value. Exporting it to $($PSStyle.Formatting.FormatAccent)`$env:$key$($PSStyle.Reset)."
                Set-Item "env:$key" -Value $remediated_value
            }
            else {
                $unrecoverable += $key
            }
        }
    }

    if ($unrecoverable) {
        throw "You must provide the values for the following configurations yourself: $($PSStyle.Formatting.ErrorAccent)$unrecoverable$($PSStyle.Formatting.Error)"
    }
}