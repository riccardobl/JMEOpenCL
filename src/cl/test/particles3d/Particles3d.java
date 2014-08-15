package cl.test.particles3d;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.stream.IntStream;

import javax.swing.JOptionPane;

import cl.test.CLLoader;
import cl.test.DatLoader;
import cl.test.DatLoader.DatOutput;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.Control;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.util.BufferUtils;

public class Particles3d extends SimpleApplication{
    private Geometry geom;
    private static int NUM_PARTICLES=800;
    private static boolean OPENCL_ENABLED, HD_PARTICLES;
    private static float DENSITY=1f;
    public static void main(String[] _a){
        OPENCL_ENABLED=JOptionPane.showConfirmDialog(null,"Would you like to run the example with OpenCL?")==0;
        HD_PARTICLES=JOptionPane.showConfirmDialog(null,"Would you like to use High Poly Particles?")==0;
        NUM_PARTICLES=Integer.parseInt(JOptionPane.showInputDialog(null,"How many particles do you want spawn?"));
        DENSITY=Float.parseFloat(JOptionPane.showInputDialog(null,"Set the particles Density (from 0.0 to 1.1)?"));

        new Particles3d().start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(200f);
        cam.setLocation(new Vector3f(0,0,-1000));
        cam.lookAt(Vector3f.ZERO,Vector3f.UNIT_Y);
        float st=1f/DENSITY;
        cam.setFrustumFar(st*10);
        assetManager.registerLoader(DatLoader.class,"dat");
        assetManager.registerLoader(CLLoader.class,"cl");
        
        DatOutput particle_shape=(DatOutput)assetManager.loadAsset(HD_PARTICLES?"monkey.dat":"cube.dat");    
        int shape_vertices_count=particle_shape.V.size();
                      
        Particle particles[]=new Particle[NUM_PARTICLES];

        IntStream.range(0,NUM_PARTICLES).parallel().forEach(i->{
            float x=FastMath.nextRandomFloat()*st*2-st;
            Particle p=new Particle(
                    new float[]{
                        x,
                        FastMath.nextRandomFloat()*st*2-st,
                        FastMath.nextRandomFloat()*st*2-st,
                    },
                    new float[]{
                        0,
                        x<0?1:0,
                        x>0?1:0,
                        .1f
                    }
            );
            p.setParticleID(i);
            particles[i]=p;
        });
      
        FloatBuffer particles_position=BufferUtils.createFloatBuffer(NUM_PARTICLES*3);
        FloatBuffer vertices_buffer=BufferUtils.createFloatBuffer(NUM_PARTICLES*shape_vertices_count*3);
        FloatBuffer colors_buffer=BufferUtils.createFloatBuffer(NUM_PARTICLES*shape_vertices_count*4);
        FloatBuffer normals_buffer=BufferUtils.createFloatBuffer(NUM_PARTICLES*particle_shape.N.size()*3);
        FloatBuffer texture_buffer=BufferUtils.createFloatBuffer(NUM_PARTICLES*particle_shape.T.size()*2);
        IntBuffer index_buffer=BufferUtils.createIntBuffer(NUM_PARTICLES*particle_shape.I.size());

        IntStream.range(0,NUM_PARTICLES).parallel().forEach(j->{
            Particle p=particles[j];
            int vp=j*3;
            particles_position.put(vp,p.getPosition()[0]);
            particles_position.put(vp+1,p.getPosition()[1]);
            particles_position.put(vp+2,p.getPosition()[2]);

            int verticesInBuffer=p.getParticleID()*shape_vertices_count;
            IntStream.range(0,particle_shape.V.size()).parallel().forEach(i->{                    
                int verticesComponentsInBuffer=(verticesInBuffer+i)*3;
                int colorComponentsInBuffer=(verticesInBuffer+i)*4;
                int textureComponentsInBuffer=(verticesInBuffer+i)*2;

                vertices_buffer.put(verticesComponentsInBuffer,particle_shape.V.get(i).x+p.getPosition()[0]);
                vertices_buffer.put(verticesComponentsInBuffer+1,particle_shape.V.get(i).y+p.getPosition()[1]);
                vertices_buffer.put(verticesComponentsInBuffer+2,particle_shape.V.get(i).z+p.getPosition()[2]);

                normals_buffer.put(verticesComponentsInBuffer,particle_shape.N.get(i).x);
                normals_buffer.put(verticesComponentsInBuffer+1,particle_shape.N.get(i).y);
                normals_buffer.put(verticesComponentsInBuffer+2,particle_shape.N.get(i).z);
                
                texture_buffer.put(textureComponentsInBuffer,particle_shape.T.get(i).x);
                texture_buffer.put(textureComponentsInBuffer+1,particle_shape.T.get(i).y);

                colors_buffer.put(colorComponentsInBuffer,p.getColor()[0]);
                colors_buffer.put(colorComponentsInBuffer+1,p.getColor()[1]);
                colors_buffer.put(colorComponentsInBuffer+2,p.getColor()[2]);
                colors_buffer.put(colorComponentsInBuffer+3,p.getColor()[3]);        
            });
            
            int indecesInBuffer=p.getParticleID()*particle_shape.I.size();
            IntStream.range(0,particle_shape.I.size()).parallel().forEach(i->index_buffer.put(indecesInBuffer+i,particle_shape.I.get(i)+verticesInBuffer));            
        });
        
        texture_buffer.rewind();
        vertices_buffer.rewind();
        normals_buffer.rewind();
        colors_buffer.rewind();
        index_buffer.rewind();
        particles_position.rewind();

        Mesh mesh=new Mesh();
        mesh.setBuffer(Type.Position,3,vertices_buffer);
        mesh.setBuffer(Type.Normal,3,normals_buffer);
        mesh.setBuffer(Type.TexCoord,2,texture_buffer);
        mesh.setBuffer(Type.Color,4,colors_buffer);
        mesh.setBuffer(Type.Index,3,index_buffer);
        mesh.updateBound();

        System.out.println("Vertices Buffer Length: "+vertices_buffer.capacity());
        System.out.println("Texture Buffer Length: "+texture_buffer.capacity());
        System.out.println("Normals Buffer Length: "+normals_buffer.capacity());
        System.out.println("Colors Buffer Length: "+colors_buffer.capacity());
        System.out.println("Indices Buffer Length: "+index_buffer.capacity());
    
        geom=new Geometry();
        geom.setMesh(mesh);
        geom.setCullHint(CullHint.Never);

        Material mat=new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setBoolean("VertexColor",true);

        Texture texture=assetManager.loadTexture(HD_PARTICLES?"monkey.png":"cllogo.png");
        texture.setMagFilter(MagFilter.Bilinear);
        texture.setMinFilter(MinFilter.Trilinear);
        mat.setTexture("ColorMap",texture);
        
        geom.setMaterial(mat);

        rootNode.attachChild(geom);

        
        
        
        
        VertexBuffer vertex_buffer=mesh.getBuffer(Type.Position);
        FloatBuffer shape_vertices=BufferUtils.createFloatBuffer(particle_shape.V.toArray(new Vector3f[0]));
        FloatBuffer gravity_point=BufferUtils.createFloatBuffer(new Vector3f(0,0,0));
        FloatBuffer velocity=BufferUtils.createFloatBuffer(NUM_PARTICLES*3);

        
        shape_vertices.rewind();
        gravity_point.rewind();
        
        if(OPENCL_ENABLED){
            CLCode cl_code=new CLCode(this,
                    velocity,
                    vertex_buffer,
                    particles_position,
                    shape_vertices,
                    shape_vertices_count,
                    gravity_point,
                    NUM_PARTICLES);
            geom.addControl(cl_code);  
        }else{
            JavaCode java_code=new JavaCode(this,
                    mesh,
                    velocity,
                    vertices_buffer,
                    particles_position,
                    shape_vertices,
                    shape_vertices_count,
                    gravity_point);
            geom.addControl(java_code);
        }
    }
    
    @Override
    public void destroy(){
        super.destroy();
        Control c=geom.getControl(JavaCode.class);
        if(c!=null)c.setSpatial(null);
        Control c2=geom.getControl(CLCode.class);
        if(c2!=null)c2.setSpatial(null);
    }
    
}
