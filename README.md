# lein-solc

A Leiningen plugin for compiling [Solidity](https://solidity.readthedocs.io/) smart contracts.

## Installation

Add to `:plugins` vector of your project.clj:

```clojure
:plugins [[lein-solc "1.0.1"]]
```
Plugin assumes [**solc** compiler](http://solidity.readthedocs.io/en/v0.4.21/installing-solidity.html) is installed and on your `$PATH`.

## Usage

You can specify the contracts to compile by adding an `:solc` map to your project.clj.
It takes the following key value pairs:
* `:src-path` string with the path where the *.sol* source files are residing, relative to the projects root path.
* `:build-path` string with the path where the compiled binaries and ABI Json files are written to, relative to the projects root directory.
* `:contracts` vector of files with the Solidity contracts source code, relative to the **src-path** directory, you can also specify sub-directories or `:all` to compile all the contracts in the root of the **src-path**.
* `:solc-err-only` boolean, if `true` only compilation errors will be reported to the **STDOUT**.
* `:verbose` (optional) boolean, if `false` the **STDOUT** output is limited to the most important information.
* `:wc` boolean, if `true` after succesfull compilation the number of characters of the compiled bin file will be printed to the **STDOUT**.

Example:

```clojure
:solc {:src-path "resources/contracts/src"
       :build-path "resources/contracts/build/"
       :contracts ["MyContract.sol" "sub/MySecondContract.sol"]
       :solc-err-only true
       :wc true}
```

The contracts in `:contracts` will be compiled when you do:

```bash
$ lein solc once
```

which is equivalent to `lein solc`. You can also watch the `:contracts` files and re-compile them on changes if you do:

```bash
$ lein solc auto
```
