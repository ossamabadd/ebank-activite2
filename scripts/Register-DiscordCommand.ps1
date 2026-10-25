param([Parameter(Mandatory = $true)][string]$GuildId)
$ErrorActionPreference = 'Stop'
if (-not $env:DISCORD_APPLICATION_ID -or -not $env:DISCORD_BOT_TOKEN) {
    throw 'Set DISCORD_APPLICATION_ID and DISCORD_BOT_TOKEN first.'
}
$body = @{
    name = 'bank'
    description = 'Poser une question a EBank'
    type = 1
    options = @(@{ name = 'question'; description = 'Votre question'; type = 3; required = $true; max_length = 4000 })
} | ConvertTo-Json -Depth 5
$url = "https://discord.com/api/v10/applications/$env:DISCORD_APPLICATION_ID/guilds/$GuildId/commands"
Invoke-RestMethod -Uri $url -Method Post -Headers @{ Authorization = "Bot $env:DISCORD_BOT_TOKEN" } -ContentType 'application/json' -Body $body | Select-Object id, name, description
