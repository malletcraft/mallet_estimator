# Build a room shell in SketchUp from measured site photos.
#
# The measurements come from annotated 360 faces (ImageMeter today, the MCFT
# app later — the JSON is the contract, not the tool that produced it). This
# script only turns numbers into geometry: floor polygon, walls up to the
# ceiling, openings cut through, beams as solids. Furniture is not its job —
# PB4 assemblies get placed into the shell afterwards, by hand or by a later
# command.
#
# Run it from SketchUp's Ruby Console (Window > Ruby Console):
#
#   load "/path/to/mallet_estimator/tools/sketchup/build_room.rb"
#   MCFT::RoomBuilder.build("/path/to/tools/sketchup/rooms/YS_MB.json")
#
# Everything lands inside one named group, so it can be deleted and rebuilt
# without disturbing anything else in the model.

require "json"

module MCFT
  module RoomBuilder
    MM = 1.0 / 25.4          # SketchUp works in inches internally
    WALL_THICKNESS_MM = 115  # a half-brick wall; only affects the shell's look

    module_function

    def mm(v)
      v.to_f * MM
    end

    def build(json_path)
      unless File.exist?(json_path)
        UI.messagebox("No room file at:\n#{json_path}")
        return nil
      end
      data = JSON.parse(File.read(json_path))
      model = Sketchup.active_model
      model.start_operation("MCFT room: #{data['room']}", true)
      begin
        group = make_room(model, data)
        model.commit_operation
        # Frame it so the person sees what was built instead of an empty view.
        model.active_view.zoom(group) if group && group.valid?
        report(data)
        group
      rescue => e
        model.abort_operation
        UI.messagebox("Room build failed: #{e.message}\n#{e.backtrace.first(4).join("\n")}")
        nil
      end
    end

    def make_room(model, data)
      poly = data["floor_polygon"]
      raise "floor_polygon needs at least 3 points" if poly.nil? || poly.length < 3

      height = data["ceiling_height"].to_f
      raise "ceiling_height is missing" if height <= 0

      root = model.active_entities.add_group
      root.name = "MCFT #{data['room_token'] || data['room']}"
      ents = root.entities

      # --- floor -------------------------------------------------------
      pts = poly.map { |x, y| Geom::Point3d.new(mm(x), mm(y), 0) }
      floor = ents.add_face(pts)
      raise "the floor points do not make a face — check floor_polygon" if floor.nil?
      floor.material = [235, 232, 225]
      floor.name = "Floor" if floor.respond_to?(:name=)

      # Walls rise from each edge. Extruding the floor would give a solid
      # block; building each wall as its own face keeps them separately
      # selectable, which is what a person wants when placing a wardrobe.
      wall_faces = {}
      wall_ids = (data["walls"] || []).each_with_object({}) do |w, h|
        h[w["edge"].to_i] = w["id"]
      end

      poly.each_with_index do |p0, i|
        p1 = poly[(i + 1) % poly.length]
        a = Geom::Point3d.new(mm(p0[0]), mm(p0[1]), 0)
        b = Geom::Point3d.new(mm(p1[0]), mm(p1[1]), 0)
        next if a.distance(b) < mm(1)          # degenerate edge

        top_a = Geom::Point3d.new(a.x, a.y, mm(height))
        top_b = Geom::Point3d.new(b.x, b.y, mm(height))
        face = ents.add_face([a, b, top_b, top_a])
        next if face.nil?

        id = wall_ids[i] || "edge#{i}"
        face.material = [246, 246, 244]
        face.name = "Wall #{id}" if face.respond_to?(:name=)
        wall_faces[id] = { face: face, a: a, b: b, index: i }
      end

      cut_openings(ents, data, wall_faces, height)
      add_beams(ents, data, wall_faces, height)
      label_room(ents, data, poly, height)
      root
    end

    # Openings are cut as real holes: a rectangle on the wall plane, erased
    # so the wall reads as a wall with a door in it rather than a wall with a
    # drawing of a door on it.
    def cut_openings(ents, data, wall_faces, height)
      (data["openings"] || []).each do |op|
        w = wall_faces[op["wall"]]
        next if w.nil?

        a, b = w[:a], w[:b]
        dir = (b - a)
        len = dir.length
        next if len == 0
        dir.normalize!

        start_at = mm(op["from_start"].to_f)
        width    = mm(op["width"].to_f)
        sill     = mm(op["sill"].to_f)
        top      = sill + mm(op["height"].to_f)
        next if width <= 0 || top <= sill

        # Refuse to run off the end of its own wall rather than making a
        # hole in the wrong place.
        if start_at < 0 || start_at + width > len + mm(1)
          puts "MCFT: skipping #{op['kind']} on #{op['wall']} — it does not fit " \
               "(#{op['from_start']}+#{op['width']} > #{(len / MM).round} mm)"
          next
        end
        top = [top, mm(height)].min

        p0 = a.offset(dir, start_at)
        p1 = a.offset(dir, start_at + width)
        quad = [
          Geom::Point3d.new(p0.x, p0.y, sill),
          Geom::Point3d.new(p1.x, p1.y, sill),
          Geom::Point3d.new(p1.x, p1.y, top),
          Geom::Point3d.new(p0.x, p0.y, top),
        ]
        hole = ents.add_face(quad)
        next if hole.nil?
        hole.erase!    # leaves the bounding edges: a real opening
        puts "MCFT: cut #{op['kind']} #{op['width']}x#{op['height']} in #{op['wall']}"
      end
    end

    # A beam is what stops a wardrobe reaching the ceiling, so it is modelled
    # as a solid rather than left as a note.
    def add_beams(ents, data, wall_faces, height)
      (data["features"] || []).select { |f| f["kind"] == "beam" }.each do |bm|
        ids = bm["at_corner"] || []
        w = wall_faces[ids.first]
        next if w.nil?

        a, b = w[:a], w[:b]
        dir = (b - a)
        next if dir.length == 0
        dir.normalize!
        # Inward normal, so the beam sits inside the room.
        inward = Geom::Vector3d.new(-dir.y, dir.x, 0)

        depth = mm(bm["width"].to_f > 0 ? bm["width"].to_f : 150)
        drop  = mm(bm["drop"].to_f)
        next if drop <= 0

        z_top = mm(height)
        z_bot = z_top - drop
        base = [
          Geom::Point3d.new(a.x, a.y, z_bot),
          Geom::Point3d.new(b.x, b.y, z_bot),
          Geom::Point3d.new(b.x + inward.x * depth, b.y + inward.y * depth, z_bot),
          Geom::Point3d.new(a.x + inward.x * depth, a.y + inward.y * depth, z_bot),
        ]
        g = ents.add_group
        g.name = "Beam #{ids.join('-')}"
        f = g.entities.add_face(base)
        next if f.nil?
        f.reverse! if f.normal.z < 0
        f.pushpull(drop)
        puts "MCFT: beam on #{ids.first}, drop #{bm['drop']} mm"
      end
    end

    def label_room(ents, data, poly, height)
      cx = poly.map { |p| p[0] }.sum / poly.length.to_f
      cy = poly.map { |p| p[1] }.sum / poly.length.to_f
      t = ents.add_text("#{data['room']} — #{data['room_token']}\n" \
                        "ceiling #{data['ceiling_height']} mm",
                        Geom::Point3d.new(mm(cx), mm(cy), mm(height)))
      t.layer = ents.model.layers[0] if t
    rescue StandardError
      nil    # a label is a nicety; never fail the build over it
    end

    def report(data)
      puts "=" * 60
      puts "MCFT room built: #{data['project']} / #{data['room']}"
      puts "source: #{(data['source'] || {})['kind']}"
      (data["caveats"] || []).each { |c| puts "  CAVEAT: #{c}" }
      puts "=" * 60
    end
  end
end
