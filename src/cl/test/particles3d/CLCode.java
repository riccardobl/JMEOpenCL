package cl.test.particles3d;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.Util;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;

import com.jme3.app.Application;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;

public class CLCode extends AbstractControl{
    protected Application APP;

    protected CLKernel CL_KERNEL;
    protected CLCommandQueue CL_QUEUE;
    protected PointerBuffer CL_WORKER;
    protected CLContext CL_CONTEXT;  
    protected CLProgram CL_PROGRAM;
    
    protected VertexBuffer VERTICES;
    protected FloatBuffer RANDOM,PARTICLES_POSITION,SHAPE_VERTICES,GRAVITY_POINT,PARTICLES_VELOCITY;
    protected int SHAPE_VERTICES_COUNT,PARTICLES_COUNT;

    protected CLMem CL_GRAVITY_POINT,CL_VERTICES,CL_PARTICLES_POSITION,CL_SHAPE_VERTICES,CL_PARTICLES_VELOCITY;
    
    public CLCode(Application app,FloatBuffer velocity,VertexBuffer v,FloatBuffer pp,FloatBuffer sv,int svc,FloatBuffer ep,int pc){
        APP=app;
        VERTICES=v;
        PARTICLES_VELOCITY=velocity;
        PARTICLES_POSITION=pp;
        SHAPE_VERTICES=sv;
        SHAPE_VERTICES_COUNT=svc;
        GRAVITY_POINT=ep;
        PARTICLES_COUNT=pc;
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        if(CL_KERNEL==null) kernelInitialize();
        else{
            CL10GL.clEnqueueAcquireGLObjects(CL_QUEUE,CL_VERTICES,null,null);
                CL_KERNEL.setArg(7,tpf);
                CL10.clEnqueueNDRangeKernel(CL_QUEUE,CL_KERNEL,1,null,CL_WORKER,null,null,null);
            CL10GL.clEnqueueReleaseGLObjects(CL_QUEUE,CL_VERTICES,null,null);
                CL10.clFinish(CL_QUEUE);
        }        
    }
    

    @Override
    public void setSpatial(Spatial spatial) {
      super.setSpatial(spatial);
      if(spatial==null&&CL_KERNEL!=null)kernelDestroy();
    }
       
    protected void kernelInitialize() {
        try{
            if(VERTICES.getId()==-1) return;
            String source=(String)APP.getAssetManager().loadAsset("particles3d.cl");
            System.out.println("OpenCL Source:\n"+source);
                        
            CL.create();
            Drawable drawable=Display.getDrawable();
            CLPlatform platform=CLPlatform.getPlatforms().get(0);
            List<CLDevice> devices=platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);

            CL_CONTEXT=CLContext.create(platform,devices,null,drawable,null);
            CL_QUEUE=CL10.clCreateCommandQueue(CL_CONTEXT,devices.get(0),CL10.CL_QUEUE_PROFILING_ENABLE,null);      
            
            CL_VERTICES=CL10GL.clCreateFromGLBuffer(CL_CONTEXT,CL10.CL_MEM_READ_WRITE,VERTICES.getId(),null);
            CL_PARTICLES_POSITION=CL10.clCreateBuffer(CL_CONTEXT,CL10.CL_MEM_READ_WRITE|CL10.CL_MEM_COPY_HOST_PTR,PARTICLES_POSITION,null);           
            CL_SHAPE_VERTICES=CL10.clCreateBuffer(CL_CONTEXT,CL10.CL_MEM_READ_ONLY|CL10.CL_MEM_COPY_HOST_PTR,SHAPE_VERTICES,null);
            CL_GRAVITY_POINT=CL10.clCreateBuffer(CL_CONTEXT,CL10.CL_MEM_READ_ONLY|CL10.CL_MEM_COPY_HOST_PTR,GRAVITY_POINT,null);
            CL_PARTICLES_VELOCITY=CL10.clCreateBuffer(CL_CONTEXT,CL10.CL_MEM_READ_WRITE|CL10.CL_MEM_COPY_HOST_PTR,PARTICLES_VELOCITY,null);

            CL_PROGRAM=CL10.clCreateProgramWithSource(CL_CONTEXT,source,null);
            Util.checkCLError(CL10.clBuildProgram(CL_PROGRAM,devices.get(0),"",null));

            CL_KERNEL=CL10.clCreateKernel(CL_PROGRAM,"main",null);       
            
            CL_KERNEL.setArg(0,CL_VERTICES);
            CL_KERNEL.setArg(1,CL_PARTICLES_POSITION);
            CL_KERNEL.setArg(2,CL_SHAPE_VERTICES);
            CL_KERNEL.setArg(3,CL_GRAVITY_POINT);
            CL_KERNEL.setArg(4,CL_PARTICLES_VELOCITY);            
            CL_KERNEL.setArg(5,SHAPE_VERTICES_COUNT);
            CL_KERNEL.setArg(6,VERTICES.getNumElements());
            CL_KERNEL.setArg(7,0);

            
            CL_WORKER=BufferUtils.createPointerBuffer(1);
            CL_WORKER.put(0,VERTICES.getNumElements());

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    protected void kernelDestroy() {
        CL_VERTICES=CL10GL.clCreateFromGLBuffer(CL_CONTEXT,CL10.CL_MEM_READ_WRITE,VERTICES.getId(),null);
        CL_PARTICLES_POSITION=CL10.clCreateBuffer(CL_CONTEXT,CL10.CL_MEM_READ_WRITE|CL10.CL_MEM_COPY_HOST_PTR,PARTICLES_POSITION,null);
        CL_SHAPE_VERTICES=CL10.clCreateBuffer(CL_CONTEXT,CL10.CL_MEM_READ_ONLY|CL10.CL_MEM_COPY_HOST_PTR,SHAPE_VERTICES,null);
        CL_GRAVITY_POINT=CL10.clCreateBuffer(CL_CONTEXT,CL10.CL_MEM_READ_ONLY|CL10.CL_MEM_COPY_HOST_PTR,GRAVITY_POINT,null);
        CL_PARTICLES_VELOCITY=CL10.clCreateBuffer(CL_CONTEXT,CL10.CL_MEM_READ_WRITE|CL10.CL_MEM_COPY_HOST_PTR,PARTICLES_VELOCITY,null);

        CL10.clReleaseMemObject(CL_VERTICES);
        CL10.clReleaseMemObject(CL_PARTICLES_POSITION);
        CL10.clReleaseMemObject(CL_SHAPE_VERTICES);
        CL10.clReleaseMemObject(CL_GRAVITY_POINT);
        CL10.clReleaseMemObject(CL_PARTICLES_VELOCITY);

        CL10.clReleaseKernel(CL_KERNEL);
        CL10.clReleaseProgram(CL_PROGRAM);
        CL10.clReleaseCommandQueue(CL_QUEUE);
        CL10.clReleaseContext(CL_CONTEXT);
        CL.destroy();
    }  
    
    
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}