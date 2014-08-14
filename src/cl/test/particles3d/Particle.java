package cl.test.particles3d;

public class Particle{
    protected float POSITION[];
    protected float COLOR[];
    protected int PARTICLE_ID;
    
    public Particle(float[] pos,float[] color){
        POSITION=pos;
        COLOR=color;
    }
    
    public void setPosition(float[] a){
        POSITION=a;
    }
    
    public float[] getPosition(){
        return POSITION;
    }    
    
    public void setColor(float[] a){
        COLOR=a;
    }
    
    public float[] getColor(){
        return COLOR;
    }
        
    public void setParticleID(int id){
        PARTICLE_ID=id;
    }
    
    public int getParticleID(){
        return PARTICLE_ID;
    }
    
}
