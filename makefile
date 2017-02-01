.PHONY: all data sass serve

all: data sass cljs

#
# Development
# ===========
#

deps:
	git submodule update --recursive --init
	npm install
prepare: deps
	node_modules/browserify/bin/cmd.js \
		-r fontfaceobserver \
		-r markdown-it \
		-r markdown-it-footnote \
		> src/bundle.js

DATA =  resources/data/do.json \
	resources/data/am.json \
	resources/data/think.json

resources/%.json: %.yaml
	mkdir -p $(@D)
	python -c 'import sys, yaml, json; \
		   json.dump(yaml.load(sys.stdin), sys.stdout, indent=4)' \
		< $< > $@

data: $(DATA)

sass:
	touch sass/main.scss
	compass compile --output-style compressed

cljs:
	lein cljsbuild once

figwheel:
	rlwrap lein figwheel

serve:
	coffee server.coffee

# pip install watchdog
watch-data:
	watchmedo shell-command \
		--recursive \
		--command="$(MAKE) data" \
		data/

watch-sass:
	compass watch

watch-cljs:
	lein cljsbuild auto debug

dev:
	trap "trap - TERM && kill 0" EXIT TERM INT; \
	$(MAKE) watch-data & \
	$(MAKE) watch-sass & \
	$(MAKE) serve & \
	$(MAKE) figwheel

#
# Deployment
# ==========
#

DEST = ~/public/
copy:
	rsync -av resources/* $(DEST)
