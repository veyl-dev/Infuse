# Original S3 resource pack

The original texture pack from the supplied S3 archive is bundled in the plugin resources as a Base64-encoded ZIP so GitHub's text-file upload path can preserve the exact binary asset.

On plugin startup, InfuseSMP S3 extracts it to:

plugins/InfuseSMP-S3/resourcepack/InfuseSMP-S3-Original.zip

The original pack metadata declares resource-pack format 64 (Minecraft 1.21.7–1.21.8). That original ZIP is not a universal 1.21-to-26.3 pack. Pack metadata and item-model definitions need version-specific adaptation before the resource pack can be advertised as compatible with every client version.

To distribute it, host the extracted ZIP at a public HTTPS URL and configure settings.resource-pack.url and settings.resource-pack.sha1 in config.yml. The current plugin extracts the pack but does not yet host it or force clients to accept it.

Keep the original pack's included textures, sounds, and attribution notices intact.
