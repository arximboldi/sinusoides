.PHONY: all

upload-all: all
	ncftpput -R -u u48595320 sinusoid.es / .

upload-html: all
	ncftpput -u u48595320 sinusoid.es / *

upload: upload-html

upload-screens: all
	ncftpput -u u48595320 sinusoid.es /screens screens/*

upload-soft: all
	ncftpput -u u48595320 sinusoid.es /soft soft/*

upload-files: all
	ncftpput -u u48595320 sinusoid.es /files files/*


