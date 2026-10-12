import megalodonte.engine.*;
import java.nio.file.*;
/** Regression: grass artwork must not create a full-cell invisible platform. */
class GroundContactTest {
 public static void main(String[] args)throws Exception{
  Game game;try(var reader=Files.newBufferedReader(Path.of("game.json"))){game=Codec.read(reader);}
  String menuTrack=MusicTrack.key(game.scenes.stream().filter(s->s.id.equals("menu")).findFirst().orElseThrow());
  for(var scene:game.scenes)if(scene.scriptClassName.equals("game.finalroz.Menu")){
   if(scene.musicAssetPath.isBlank()||!menuTrack.equals(MusicTrack.key(scene)))throw new AssertionError("Menus must share uninterrupted music: "+scene.id);
  }
  var source=game.scenes.stream().filter(s->s.id.equals("level-1")).findFirst().orElseThrow();
  Game single=new Game();single.scenes.add(source);single.entrySceneId=source.id;
  try(var runtime=new megalodonte.engine.Runtime(single,name->name.endsWith("Enemy")?megalodonte.engine.Runtime.reflection(GroundContactTest.class.getClassLoader()).create(name):new GameScript(){},new megalodonte.engine.Runtime.Events(){})){
   for(int i=0;i<180;i++)runtime.update(1./60,0,0);
   for(String name:new String[]{"Zombie3","Zombie5"}){
    var enemy=runtime.scene.objects.stream().filter(e->e.name.equals(name)).findFirst().orElseThrow();
    var bounds=megalodonte.engine.Runtime.collisionBounds(runtime.scene,enemy);
    if(Math.abs(bounds.y()-30)>.01||!runtime.blocked(enemy,0,-.01))throw new AssertionError(name+" must stand on real ground: "+bounds);
   }
  }
  int cells=0;
  for(var scene:game.scenes){
   if(scene.tileMaps.stream().noneMatch(m->m.tiles.values().stream().anyMatch(t->t.assetPath.equals("assets/78cff35fac9d9875eb7f.png")&&t.sourceX==128&&t.sourceY==256)))continue;
   Game document=new Game();document.scenes.add(scene);document.entrySceneId=scene.id;
   try(var runtime=new megalodonte.engine.Runtime(document,n->new GameScript(){},new megalodonte.engine.Runtime.Events(){})){
    for(var map:runtime.scene.tileMaps)for(var layer:map.layers)for(int i=0;i<layer.data.length;i++){
     var tile=map.tiles.get((int)layer.data[i]);if(tile==null||!tile.assetPath.equals("assets/78cff35fac9d9875eb7f.png")||tile.sourceX!=128||tile.sourceY!=256)continue;
     var probe=new Game.Entity();probe.width=.05;probe.height=.05;
     probe.x=map.x+(i%map.columns+.5)*map.tileWidth*map.unitScale;
     double base=map.y+(map.rows-1-i/map.columns)*map.tileHeight*map.unitScale;
     probe.y=base+.8*map.tileHeight*map.unitScale;
     var area=megalodonte.engine.Runtime.collisionBounds(runtime.scene,probe);
     for(var obstacle:map.obstacles){
      var rect=new megalodonte.engine.Runtime.CollisionRect(map.x+obstacle.x*map.unitScale,map.y+obstacle.y*map.unitScale,obstacle.width*map.unitScale,obstacle.height*map.unitScale);
      if(megalodonte.engine.Runtime.overlaps(area,rect))throw new AssertionError("Invisible grass platform: "+scene.id+"/"+i);
     }
     cells++;
    }
   }
  }
  if(cells!=53)throw new AssertionError("Expected 53 decorative grass cells, got "+cells);
  System.out.println("PASS: 53 grass cells swept across campaign");
  System.out.println("PASS: outside zombies stand on real ground, not transparent grass");
 }
}
