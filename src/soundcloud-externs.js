/* Externs for the SoundCloud widget API, loaded at runtime from
 * https://w.soundcloud.com/player/api.js
 *
 * The release build runs Closure with :advanced, which would otherwise
 * rename these calls out from under the script. */

var SC = {};

/** @constructor */
SC.Widget = function (element) {};

SC.Widget.Events = {};
SC.Widget.Events.READY = "";
SC.Widget.Events.PLAY = "";
SC.Widget.Events.PAUSE = "";
SC.Widget.Events.FINISH = "";
SC.Widget.Events.ERROR = "";
SC.Widget.Events.PLAY_PROGRESS = "";
SC.Widget.Events.LOAD_PROGRESS = "";

SC.Widget.prototype.bind = function (event, listener) {};
SC.Widget.prototype.unbind = function (event) {};
SC.Widget.prototype.load = function (url, options) {};
SC.Widget.prototype.play = function () {};
SC.Widget.prototype.pause = function () {};
SC.Widget.prototype.seekTo = function (milliseconds) {};
SC.Widget.prototype.getDuration = function (callback) {};
SC.Widget.prototype.getCurrentSound = function (callback) {};

/* Options accepted by SC.Widget.prototype.load. */
var SCWidgetLoadOptions = {};
SCWidgetLoadOptions.auto_play;
SCWidgetLoadOptions.callback;
