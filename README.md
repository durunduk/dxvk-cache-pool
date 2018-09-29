# DXVK cache pool (Beta)

Client/server to share DXVK pipeline caches.

Client:
- Fetches missing DxvkStateCacheEntry's and patches the .dxvk-cache.
- Submits local DxvkStateCacheEntry's not present on server.

Server:
- Provides REST interface to access caches.


Not affiliated with the DXVK project, please don't blame him if this destroys your cache files.

## Building

Prerequisites:
- maven 3
- openjdk >= 8

Build: 
```bash
./build.sh
```

Executables:
```bash
dxvk-cache-client
dxvk-cache-server
```

Archlinux:

See [PKGBUILD](arch/PKGBUILD)


## Usage

### Client

```bash
$ ./dxvk-cache-client -h
usage: dvxk-cache-client  directory... [-h] [--host <url>] [-t <path>]
       [--verbose]
 -h,--help            show this help
    --host <url>      Server URL
 -t,--target <path>   Target path to store caches
    --verbose         verbose output
```

#### Environment
For everything to work you should set DXVK_STATE_CACHE_PATH as a global variable and point it to the directory you want to store your .dxvk-cache files in.

See [dxvk.sh](dxvk.sh) for an example you can put directly into `/etc/profile.d/`.

`dxvk-cache-client` will use DXVK_STATE_CACHE_PATH if defined, but you can override it with `-t`.

#### Example

Assuming you store your wine prefixes in `/usr/local/games/wine`, you can run it like:

```bash
$ ./dxvk-cache-client /usr/local/games/wine
scanning directories
scanned 77522 files
looking up state caches for 235 possible games
found 3 matching caches
writing 1 new caches
 -> writing witcher3 to /home/poison/.cache/dxvk/witcher3.dxvk-cache
updating 2 caches
 -> ManiaPlanet is up to date with 473 entries
 -> ManiaPlanet sending 31 missing entries to remote
 -> patching Beat Saber with 521 entries, adding 32 entries
found 0 candidates for upload
```

It will search for exe files in the passed directories and update the .dxvk-cache's for you.


### Server
```bash
$ ./dxvk-cache-server -h
usage: dvxk-cache-server [-h] [--port <port>] [--storage <path>]
       [--versions <version>]
 -h,--help                 show this help
    --port <port>          Server port
    --storage <path>       Storage path
    --versions <version>   DXVK state cache versions to accept
```

## Implementation problems

### Identifying a game

Possible Solutions:

- Just the exe's filename. After a bit of discussion the only possible choice: https://github.com/doitsujin/dxvk/issues/677
- ~~SHA1 of the exe.~~ Don't want to loose the cache if the application is updated. Games built using an engine can have the same exact binary.
- ~~Steam game id.~~ The most robust and my preferred solution, but would make it exclusive to Steam.
- ~~Exe name plus parent directory.~~ ~~Still suboptimal but right now what I opted for. Assumes users don't go around changing the installation folder name. Should work well for Steam.~~


### Security

There is none.

- Anybody can submit entries. Nothing prevents cache poisoning. Even authentication and a network of trust would be of little help as:
- Currently there is no way to validate the DxvkStateCacheEntry struct and its members. Doing so would be hard to impossible.

