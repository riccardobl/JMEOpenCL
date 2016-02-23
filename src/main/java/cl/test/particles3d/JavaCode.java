package cl.test.particles3d;

import java.nio.FloatBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.IntStream;

import com.jme3.app.Application;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.AbstractControl;

public class JavaCode extends AbstractControl{
    protected Application APP;

    protected FloatBuffer VERTICES,PARTICLES_POSITION,SHAPE_VERTICES,GRAVITY_POINT,PARTICLES_VELOCITY;
    protected int SHAPE_VERTICES_COUNT;
    protected Mesh MESH;
    
    protected ScheduledThreadPoolExecutor THREAD_POOL=new ScheduledThreadPoolExecutor(1);
    public JavaCode(Application app,Mesh m,FloatBuffer velocity,FloatBuffer v,FloatBuffer pp,FloatBuffer sv,int svc,FloatBuffer ep){
        APP=app;
        VERTICES=v;
        PARTICLES_VELOCITY=velocity;
        PARTICLES_POSITION=pp;
        SHAPE_VERTICES=sv;
        SHAPE_VERTICES_COUNT=svc;
        GRAVITY_POINT=ep;
        MESH=m;
    }
    
    @Override
    public void setSpatial(Spatial s){
        super.setSpatial(s);
        if(s==null)THREAD_POOL.shutdown();
    }
    
    protected boolean READY=true;
    @Override
    protected void controlUpdate(float tpf) {
        if(READY){
            READY=false;
            THREAD_POOL.submit(()->{     
                final Vector3f gravity=new Vector3f(GRAVITY_POINT.get(0),GRAVITY_POINT.get(1),GRAVITY_POINT.get(2));
                IntStream.range(0,VERTICES.capacity()/3).parallel().forEach(i->{           
                    int particle_id=i/SHAPE_VERTICES_COUNT;
                    int particle_pos_fist_component=particle_id*3;
                    Vector3f particle_pos=new Vector3f(
                            PARTICLES_POSITION.get(particle_pos_fist_component),
                            PARTICLES_POSITION.get(particle_pos_fist_component+1),
                            PARTICLES_POSITION.get(particle_pos_fist_component+2)
                    );
               
                    if(i%SHAPE_VERTICES_COUNT==0){
    
                        Vector3f acceleration=gravity.subtract(particle_pos).normalizeLocal().mult(tpf);
                        
                        Vector3f velocity=new Vector3f(
                                PARTICLES_VELOCITY.get(particle_pos_fist_component),
                                PARTICLES_VELOCITY.get(particle_pos_fist_component+1),
                                PARTICLES_VELOCITY.get(particle_pos_fist_component+2)
                        );
                        velocity.addLocal(acceleration);
                        if(velocity.x>100)velocity.x=100;
                        if(velocity.y>100)velocity.y=100;
                        if(velocity.z>100)velocity.z=100;
                        
                        PARTICLES_VELOCITY.put(particle_pos_fist_component,velocity.x);
                        PARTICLES_VELOCITY.put(particle_pos_fist_component+1,velocity.y);
                        PARTICLES_VELOCITY.put(particle_pos_fist_component+2,velocity.z);
                        
                        PARTICLES_POSITION.put(particle_pos_fist_component,particle_pos.x+velocity.x);
                        PARTICLES_POSITION.put(particle_pos_fist_component+1,particle_pos.y+velocity.y);
                        PARTICLES_POSITION.put(particle_pos_fist_component+2,particle_pos.z+velocity.z);
                        
                    }
                    
                    int shape_vertex_id=(i-particle_id*SHAPE_VERTICES_COUNT)*3;
        
                    Vector3f shape_vertex_pos=new Vector3f(
                            SHAPE_VERTICES.get(shape_vertex_id),
                            SHAPE_VERTICES.get(shape_vertex_id+1),
                            SHAPE_VERTICES.get(shape_vertex_id+2)
                    );
                    
                    int vid=i*3;
                    VERTICES.put(vid,particle_pos.x+shape_vertex_pos.x);
                    VERTICES.put(vid+1,particle_pos.y+shape_vertex_pos.y);
                    VERTICES.put(vid+2,particle_pos.z+shape_vertex_pos.z);
        
                });
                APP.enqueue(()->{
                    MESH.setBuffer(Type.Position,3,VERTICES);
                    READY=true;
                    return null;
                });
            });
        }
    }
    
    
  
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}