[![Clojars Project](https://img.shields.io/clojars/v/io.github.vlaaad/cljfx-flowless.svg)](https://clojars.org/io.github.vlaaad/cljfx-flowless)

# cljfx-flowless

Cljfx-flowless is an idiomatic wrapper of [Flowless](https://github.com/FXMisc/Flowless) 
library — an efficient virtual flow implementation similar to list view.

# Requirements

cljfx/flowless requires Java 11.

# Usage

A minimal code to try it out:

```clojure
(require '[cljfx.api :as fx]
         '[cljfx.flowless :as fx.flowless])

(fx/on-fx-thread
  (fx/create-component
    {:fx/type :stage 
     :showing true
     :scene {:fx/type :scene 
             :root {:fx/type fx.flowless/virtualized-scroll-pane ;; add scroll bars
                    :content {:fx/type fx.flowless/virtual-flow
                              :cell-factory identity
                              :items (for [i (range 1000)]
                                       {:fx/type :label :text (str i)})}}}}))
```

# Documentation

## virtual-flow

This lifecycle defines a VirtualFlow.

Supported props:
- `:items` - list of any objects.
- `:cell-factory` - a function that converts item to cljfx description of any node
  that will display the item. Required, can't be changed.
- `:orientation` - either `:vertical` (default) or `:horizontal`.
- `:gravity` - either `:front` (default) or `:rear`, defines alignment of cell when
  they don't fill the whole view.
- other region props

## virtualized-scroll-pane

This lifecycle defines a node that wraps VirtualFlow pane and shows it with scroll bars.
Supported props:
- `:content` (required) - component description that defines a VirtualFlow instance. 
  Can't be changed.
- `:hbar-policy` and `:vbar-policy` - scroll bar policies, either `:never`, `:always` 
  or `:as-needed` (default).
- other region props.

