# layervault-dl

LayerVault is shutting down. This utility downloads and saves the most recent
file for each revision cluster in your account.

## Usage

Generate an access token using the instructions
[on LayerVault](https://developers.layervault.com/). Then, get the
locations of the files we want to download and dump them into a text file:
    lein repl
    (download-layervault-map "./temp-map.txt" "YOUR_AUTH_TOKEN")
Then, read the text file and actually download the files:
    (download-layervault-files "./temp-map.txt" "./path/to/dump/directory" "YOUR_AUTH_TOKEN")
