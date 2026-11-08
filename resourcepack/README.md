# Original S3 resource pack

The original texture pack from the supplied S3 archive is bundled in the plugin resources as a Base64-encoded ZIP so GitHub's text-file upload path can preserve the exact binary asset.

On plugin startup, InfuseSMP S3 creates a server-version-specific ZIP in:

plugins/InfuseSMP-S3/resourcepack/

The generated file is named InfuseSMP-S3-<server-version>.zip. The adapter updates the resource-pack metadata for the detected Minecraft version, maps the original S3 potion model data IDs (414001–414013), and uses legacy item-model overrides on 1.21.0–1.21.3. Newer versions use the modern item definition layout. Wind-charge model files are only kept on versions that understand their model format.

The supplied source pack declared format 64, which corresponds to Minecraft 1.21.7–1.21.8. Newer Minecraft releases use different pack metadata formats, including 97.1 for 26.3. The adapter prepares a version-specific pack, but each client version still needs an in-game visual check before the pack can be called fully tested.

To distribute it, host the generated ZIP at a public HTTPS URL and configure settings.resource-pack.url and settings.resource-pack.sha1 in config.yml. Set settings.resource-pack.enabled to true to prompt players on join. The plugin extracts and adapts the pack; it does not host the ZIP itself.

Keep the original pack's included textures, sounds, and attribution notices intact.
