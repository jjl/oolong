# oolong

A config-based loader for stuartsierra's `component` library

## Aims:

* Be really simple to use
* Be machine manipulable
* Keep your app's config in one configuration file
* Be secure (future plans and all that...)

## Background

From [Wikipedia](https://en.wikipedia.org/wiki/Oolong):

> Oolong is a traditional Chinese tea produced through a unique process
> including withering the plant under the strong sun and oxidation before
> curling and twisting.
> ...
> It can be sweet and fruity with honey aromas, or woody and thick with
> roasted aromas, or green and fresh with bouquet aromas, all depending
> on the horticulture and style of production

`oolong` is a config-based loader for stuartsierra's `component` library
It treats config files through a unique process including withering
the amount of code you have to write and providing for automated
manipulation of your configuration

oolong's main purpose is to take a descriptor of your application and give you something you can run. It does this by ascribing meaning to a structured configuration (we favour edn stored as a file).

Complex systems cannot run without code being written and we make no attempt to prevent you from plugging in custom code - we only aim to provide a simple means of declaring the *structure* of your application - how your code fits together.


## Usage

Sample project.clj:
```clojure
{:dependencies [[oolong "0.1.0"]
                [org.clojure/tools.reader "0.8.16"]]}
```

Sample loader code:

```clojure
(ns myapp.core
  (:require [oolong :refer [brew-master-file start-system]]))
(defn go
  "Given a filename, reads the file as edn and loads the services
   named under the `:app` key", passing in the entire config
  [filename]
  (-> filename brew-master-file start-system))
```

Sample component code:

```clojure
(ns myapp.cpt.foo
  (:require [oolong :refer [Lifecycle]]))

; This is the component record itself
(defrecord Foo [a bar]
  ; The Lifecycle protocol is what a component obeys
  Lifecycle

  ; The start method is called when you start the whole system up
  ; Dependencies will be resolved automatically and merged into self
  ; self will thus contain all dependencies and its configuration
  (start [{:keys [a bar] :as self}]
    ; Here 'a' is from the config, 'bar' is a component we need
    ; And you'll almost certainly want to do something more useful here
    ; like connect to a database or something
    (assoc-in self [:a2] a))
    
  ; The stop method is called when you stop the whole system
  ; Here we'd disconnect any resources we've acquired and assoc nil to them
  (stop [self]
    (assoc-in self [:a2] nil)))

(defn cpt
  "Constructor function for Foo record.
  Args: [config] config map
  Returns: new Foo record
  [config]
  (map->Foo config))
```

Sample configuration (edn):

```edn
; `:app` points at a service descriptor. Every entry in the map names:
; - A key in the configuration to restrict scope to
; - A key in the service that names the system or component following
; The `cpt` form is used to load a named component
{:app {:foo (cpt myapp.cpt.foo/cpt :bar)
       :bar (cpt myapp.cpt.bar/cpt)}
 :foo {:a 123}
 :bar {}}
```

## Documentation

### Quickstart

There are services and components. Both are structured as records. Services provide a way of grouping components together.

In the general case, services do not need to do anything special to initialise their components so we specify them simply as a map of name to descriptor

```edn
{:foo (cpt myapp.cpt.foo/cpt :bar)
 :bar (cpt myapp.cpt.bar/cpt)}
```

Here the service will have two keys, `:foo` and `:bar`, each pointing to a component. We declare a component with a list form. The simplest example of which can be seen for the `:bar` key, where we name `myapp.cpt.bar/cpt`. This expects the symbol `myapp.cpt.bar/cpt` to exist and be a function that takes a single argument (which will be the configuration under the key `:bar`) and returns a component record.

The component form for `:foo` specifies a second argument: a dependency descriptor. In this case we use `:bar`, which declares a dependency on `:bar`. Also valid are a vector (for multiple dependencies) and a map (to support renaming of keys). Please see the README.md in the [component repository](https://github.com/stuartsierra/component/) for more information about this form.

You can also nest systems like this:
```edn
{:foo {:bar (cpt myapp.cpt.bar/cpt)}}
```

In this case, since we traverse the `:foo` and `:bar` keys, the component's config will be found in the path `[:foo :bar]` in the configuration file, like so:

```edn
{:foo {:bar {:is-nested? true}}}
```

Sometimes you will want systems that depend on other systems. You can do that with the `sys` form
```edn
{:foo (sys {:bar myapp.cpt.bar} :baz)
 :baz myapp.cpt.baz}
```

Inserting the `sys` form here means we have introduced another map, so in the above, you will provide configuration that looks like this:
```edn
{:foo {:bar {:is-nested? true}}
 :baz {:is-top-level? true}}
```

Anywhere you can provide a map in your system configuration, you can also just provide a namespace-qualified symbol pointing to a function. This function will be loaded and called with the system configuration attached. If we take this system:
```edn
{:foo `myapp.sys.bar}
```

Then we expect to load configuration from the `:foo` key in the configuration. We do not negate the configuration-nested effects of using a map to describe a system.

We recommend embedding your service configuration in your main (and only) configuration file.

Further information about how to use @stuartsierra's excellent `component](https://github.com/stuartsierra/component/) library can be found in the README.md of the [component repository](https://github.com/stuartsierra/component/)

### API Documentation

Marginalia docs (docs/code split view) can be found in the 'docs' directory or on github: https://github.com/jjl/oolong/tree/master/docs/index.html

They can be regenerated with `lein docs`

Codox documentation (just the docs) is forthcoming.

### Future

A few things to do:
* Tweak marginalia docs
* Also produce codox docs
* Spit out line and column information on error
* Prepare a really nice theme for both marginalia and codox
* Move to inline testing without breaking marginalia output

## License

Copyright © 2015 James Laver

Distributed under the MIT License (see file `LICENSE` in this directory)
