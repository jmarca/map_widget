# Map widget

## Refactoring Sep 2016

component died, and this widget never took off anyway, so time to
refactor and recode.  At the moment, I'm aiming for clojurescript, and
standard leaflet mapping, I think.



# older write up
Sifting though my old code, I do not see a clear set of widgets for
building map-based web sites, even though I do have running sites.
This is an attempt to start building web components using component.js
approach.

This widget is a mapping widget.  The idea is to pull in some mapping
library (aiming for d3, but will backslide to leaflet, or whatever)
then put my geojson layer on top of it.

At the moment the intent is to show an example.  I have no idea how to
make this a pluggable generic tool at the moment, but ideally I would
just list API end points for geojson services and have them all show
up as layers that you can click on and off.
