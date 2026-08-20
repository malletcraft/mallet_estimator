# Room shells from measured site photos

Turns the measurements marked on a room's six 360 faces into a SketchUp
room shell — floor, walls, openings cut through, beams as solids — so the
PB4 assemblies get placed into a real room instead of a guessed box.

The contract is the **JSON**, not the tool that produced it. Today the
numbers come from ImageMeter annotations read off the faces; when the MCFT
app's own annotation is good enough it will write the same JSON, and
nothing here changes.

## Run it

In SketchUp: **Window → Ruby Console**, then two lines:

```ruby
load "/Users/<you>/mallet_estimator/tools/sketchup/build_room.rb"
MCFT::RoomBuilder.build("/Users/<you>/mallet_estimator/tools/sketchup/rooms/YS_MB.json")
```

Everything lands in one named group (`MCFT YS_MB`). Delete the group and
re-run to rebuild — nothing else in the model is touched.

## Check a room file before building

```
python3 tools/sketchup/validate_room.py tools/sketchup/rooms/YS_MB.json
```

It refuses outlines that cross themselves, compares the outline's perimeter
against what was measured on site, and checks every opening actually fits
on the wall it claims. A mistake caught here is a number; the same mistake
inside SketchUp is a wrong room nobody notices.

## The JSON

```jsonc
{
  "units": "mm",                    // always mm, like every other number here
  "ceiling_height": 2670,
  "floor_polygon": [[0,0], ...],    // clockwise, mm, room's own coordinates
  "walls":   [{"id": "back", "edge": 0}],   // edge = index into floor_polygon
  "openings":[{"kind": "door", "wall": "back", "from_start": 120,
               "width": 890, "sill": 0, "height": 2051}],
  "features":[{"kind": "beam", "at_corner": ["back","right"],
               "drop": 320, "width": 140}],
  "caveats": ["anything the photos could not settle"]
}
```

`caveats` is not decoration. A photo gives lengths, not compass bearings,
so anything inferred rather than measured belongs there and gets printed in
the Ruby Console every time the room is built.
