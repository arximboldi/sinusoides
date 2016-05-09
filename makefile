.PHONY: all data sass serve

all: data sass cljs

#
# Development
# ===========
#

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
	compass compile

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

upload: upload-root upload-js upload-css upload-data upload-views upload-static

upload-root:
	ncftpput -z -R -f host.ncftpput / resources/.htaccess resources/favicon.ico
upload-js:
	ncftpput -z -m -R -f host.ncftpput /js resources/js/*
upload-css:
	ncftpput -z -m -R -f host.ncftpput /css resources/css/*
upload-data:
	ncftpput -z -m -R -f host.ncftpput /data resources/data/*
upload-views:
	ncftpput -z -m -R -f host.ncftpput /views resources/views/*
upload-pic:
	ncftpput -z -m -R -f host.ncftpput /static/pic resources/static/pic/*
upload-static:
	ncftpput -z -m -R -f host.ncftpput /static resources/static/*
