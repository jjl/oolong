The Irresponsible Clojure Guild presents...

# oolong

A config-based loader for Stuart Sierra's `component` library

## Aims:

* Be really simple to use
* Be machine manipulable
* Keep your app's config in one file
* Be secure (future plans and all that...)
* Support ClojureScript (with [gotchas](#clojurescript-supportgotchas))

## Status: Sunset

This library is in sunset mode: active work is not taking place, we will still accept bugfixes.

We strongly recommend you use [codependence](https://github.com/irresponsible/codependence/) instead

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

project.clj:

[![Clojars Project](http://clojars.org/irresponsible/oolong/latest-version.svg)](http://clojars.org/irresponsible/oolong)

Sample loader code:

```clojure
(ns myapp.core
  (:require [irresponsible.oolong :refer [brew-file start-system]]))
(defn go
  "Given a filename, reads the file as edn and loads the services
   named under the `:app` key, passing in the entire config"
  [filename]
  (-> filename brew-file start-system))
```

Sample component code:

```clojure
(ns myapp.cpt.foo
  (:require [com.stuartsierra.component :refer [Lifecycle]]))

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
  Returns: new Foo record"
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

## We make components too!

Need a database pool for your PostgreSQL-using app? instance? No problem!
[utrecht](https://github.com/irresponsible/utrecht) packs HikariCP
into an oolong-ready component for your sql pleasure.

More coming soon!

## Documentation

### API Changes

`brew-master-file` is now known as `brew-file` and `brew-master` is now known as `brew`. There are (deprecated) aliases present in the namespace for now, but they will be removed when we hit 1.0

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

In this case, since we traverse the `:foo` and `:bar` keys, the component's config will be found in the path `[:foo :bar]` in the configuration file, so to specify the `:is-nested?` property in this component, we wcould do this:

```edn
{:foo {:bar {:is-nested? true}}}
```

Sometimes you will want systems that depend on other systems. You can do that with the `sys` form
```ednp
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
{:foo `myapp.sys/bar}
```

`myapp.sys/bar` will now be applied to whatever is in the `:foo` key. The same key nesting thing goes on as for systems and components. This does not allow you to specify dependencies.

We recommend embedding your service configuration in your main (and only) configuration file and checking it into source control.

Further information about how to use @stuartsierra's excellent [component](https://github.com/stuartsierra/component/) library can be found in its README.md.

### Functions and non-components

We saw earlier that you can use `(cpt ...)` and `(sys ...)` to manufacture components and systems. You can also just embed a qualified symbol directly, which has the effect that it should name a function which is called with its paired config. This will not allow you to declare a dependency on something else. If you're not reading from a file, you can also provide an actual function (from `fn`!) anywhere you would provide a symbol (which is particularly useful in clojurescript where you can't load from a file!).

### ClojureScript support/gotchas

The clojure interface was carefully chosen to simply require a config file and a call to a single function which would load everything and deal with starting it up.
In ClojureScript, file access means making more http requests, which means building more outputs, which means faffing with build tools and really it doesn't make anybody happy. For that reason, in ClojureScript, we do not support the `brew-file` function, instead supporting only `brew`, to which you can directly provide the data you would normally provide from a config file. We also support embedding functions wherever named symbols are supported to make it easier.

The other gotcha is that since we can't require namespaces in ClojureScript at runtime, we cannot autoload namespaces (because of the last paragraph in files). To this end, any namespaces which you refer to in your config must already have been included in your build (see in our test suite where we include the test components, but only for cljs). This isn't as much of a burden as it sounds because you're ultimately wanting to bundle all the javascript into a single file anyway and you can spit out your state into the page and read it back in ClojureScript. It's what I consider the most sensible way of developing ClojureScript with this library, and the api remains simple.

Finally, cljs seems to take objection to me reexporting stuart's Lifecycle record and the associated start/stop methods. No idea why, but since it doesn't seem to work I only reexport them in clojure: please import them from component directly.

```cljs
(ns myapp.cljs.demo
  (:require [irresponsible.oolong :refer [brew]]
            [myapp.cljs.cpt.a])) ;; unlike clj, we have to include it
  
(def master
  {:app {:a myapp.cljs.cpt.a/cpt}
   :a {:foo? true}}

(def services (brew master))
```

### API Documentation

Coming very very very soon. Use this README for now and have a look at the code if necessary.

### Future

A few things to do:
* Docs
* Spit out line and column information on error

## License

Copyright © 2015 James Laver

Distributed under the MIT License (see file `LICENSE` in this directory)
