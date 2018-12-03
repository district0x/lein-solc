# lein-solc

[![Build Status](https://travis-ci.org/district0x/lein-solc.svg?branch=master)](https://travis-ci.org/district0x/lein-solc)

A Leiningen plugin for compiling [Solidity](https://solidity.readthedocs.io/) smart contracts.

## Installation

lein-solc is available from Clojars. The latest released version is: <br>
[![Clojars Project](https://img.shields.io/clojars/v/lein-solc.svg)](https://clojars.org/lein-solc) <br>
Add it to the `:plugins` vector of your project.clj.

Plugin assumes [**solc** compiler](http://solidity.readthedocs.io/en/v0.4.21/installing-solidity.html) is installed and on your `$PATH`.

## Usage

You can specify the contracts to compile by adding an `:solc` map to your project.clj.
It takes the following key value pairs:
* `:src-path` string with the path where the *.sol* source files are residing, relative to the projects root path.
* `:build-path` string with the path where the compiled output is written to, relative to the projects root directory.
* `:abi?` (optional) boolean, if `true` (default) output includes contract's abi interfaces. 
* `:bin?` (optional) boolean, if `true` (default) output includes contract's bytecode. 
* `:truffle-artifacts?` (optional) boolean, if `true` contracts are compiled into [truffle artifacts](https://truffleframework.com/docs/truffle/getting-started/running-migrations#artifacts-require-), that can be required and deployed with truffle. 
* `:contracts` vector of files with the Solidity contracts source code, relative to the **src-path** directory (you can also specify sub-directories), or `:all` to compile all of the contracts in the root of the **src-path**.
* `:solc-err-only` (optional) boolean, if `true` (default value) only compilation errors will be reported to the **STDOUT**.
* `:verbose` (optional) boolean, if `false` (default value) the **STDOUT** output is limited to the most important information.
* `:byte-count` boolean, if `true` after succesfull compilation the number of bytes in the compiled `.bin` file will be printed to the **STDOUT**. Only valid if `:bin?` is set to `true`.
* `:optimize-runs` (optional) map of contract filenames and parameter `n` values, where `0 <= n < inf` is an estimated number of contract runs for optimizer tuning. It represents a trade-off between a smaller bytecode (low values) and cheaper transaction costs (high values).

Example:

```clojure
:solc {:src-path "resources/contracts/src"
       :build-path "resources/contracts/build/"
       :contracts ["MyContract.sol" "sub/MySecondContract.sol"]
       :byte-count true
       :optimize-runs {"MyContract.sol" 1}}
```

The contracts in `:contracts` will be compiled when you do:

```bash
$ lein solc once
```

which is equivalent to `lein solc`. You can also watch the `:contracts` files and re-compile them on changes if you do:

```bash
$ lein solc auto
```
