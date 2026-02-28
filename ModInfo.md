# Rust-MC Mod Compatibility List

This document lists all mods that Rust-MC currently tracks or plans to track for Mixin support, compatibility patching, or API integration.

Currently Supported / Implemented
These mods have active compatibility hooks, native code dispatching, or mixin disabling logic implemented within Rust-MC.

Sodium Tracked in ModBridge. Base visual compatibility.
Lithium Tracked in ModBridge. Base optimization compatibility.
Starlight Tracked in ModBridge. Rust-MC disables native lighting hooks if Starlight is present to prevent conflicts.
FerriteCore Tracked in ModBridge. Rust-MC disables native lighting hooks if FerriteCore is present (as it alters memory models heavily).
ScalableLux Tracked in ModBridge and ScalableLuxCompat. Rust-MC disables native lighting hooks and hooks into ScalableLux's fast-path API.
MoreCulling Tracked in ModBridge.
Distant Horizons Tracked via DistantHorizonsCompat. Rust-MC automatically disables Distant Horizon's chunk fade-in via reflection to optimize rendering.
MiniHUD Handled via RenderUtilsMixin. Rust-MC culls MiniHUD wireframe shapes that fall outside the vanilla render distance.
RRLS (Remove Reloading Screen)
Implemented via SplashOverlayMixin and LevelLoadingScreenMixin to gracefully bypass loading screens where unnecessary, allowing background loads.
Planned / To Be Implemented
These mods are planned for future integration, specialized Mixins, or config-driven features.

Reese's Sodium Options / Sodium Extra
Planned: Integrate Rust-MC's visual configurations (like Culling toggles) directly into the Sodium video settings screen.
(This list is dynamically updated as new integrations are built).

accurateblockplacement
ambientenvironment
appleskin
architectury
audio_engine_tweaks
authme
badoptimizations
balm
betteradvancements
betterbiomeblend
betterblockentities
betterf3
bettermounthud
betterstats
bettertab
c2me
capes
cbbg
chat_heads
chatpatches
clearwaterlava
clickthrough
clienthings
clientsort
clienttweaks
cloth-config
clumps
collective
collisionfix
combinedpacks
commandkeys
config_editor
configurable
continuity
controlling
crash_assistant
creativecore
darkgraph
debugify
detailabreconst
dhmi
distanthorizons
durabilitytooltip
durabilityviewer
dynamic_fps
early_loading_bar
emojitype
enchantmentlevelcapindicator
entity-view-distance
entity_model_features
entity_texture_features
entityculling
essential-container
extremesoundmuffler
fabric-api
fabric-language-kotlin
fabricloader
fallingleaves
fastipping
ferritecore
fix-mc-stats
forcecloseworldloadingscreen
forgeconfigapiport
freecam
fusion
fzzy_config
gnetum
held-item-info
immediatelyfast
iris
ixeris
jade
jeb
lambdynlights
libjf
lighty
litematica
litematica_printer
lithium
locator-heads
logarithmic-volume-control
malilib
maplink
mcqoy
minihud
mixintrace
modernfix
modmenu
moreculling
moremousetweaks
mousetweaks
mtfd
no-telemetry
nochatreports
nopackcompatcheck
noreportbutton
notenoughanimations
notenoughcrashes
obfuscation_improver
optipainting
oxidizium
packetfixer
particle-visor
particle_core
particlerain
pchf
placeholder-api
presencefootsteps
puzzleslib
quick-pack
raknetify
reeses-sodium-options
resourcify
ridingmousefix
rrls
rsls
sciophobia
searchables
secret-items-tab
seedmapper
servercore
serverlistfix
serverpingerfixer
shieldfixes
shieldstatus
skinlayers3d
smart_particles
smoothmaps
smoothtexturefix
sodium
sodium-extra
sodium-fullbright
sound_physics_remastered
spark
stack-to-nearby-chests
stackdeobfuscator
status-effect-bars
stfu
substrate
supermartijn642configlib
switcheroo
tcdcommons
tick-sync
tooltipscroll
translucent-glass
tweakermore
tweakeroo
ukulib
vertigo
viafabricplus
visuality
vmp
walksylib
wi_zoom
wildfire_gender
xaerominimap
xaeroplus
xaeroworldmap
xaerozoomout
yeetusexperimentus
yet_another_config_lib_v3
