package cl.test.simpletest;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
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

import com.jme3.app.SimpleApplication;
import com.jme3.math.FastMath;

public class OpenCLSimpleTest extends SimpleApplication{
    public static void main(String[] args) throws LWJGLException {
        new OpenCLSimpleTest().start();
    }

    public OpenCLSimpleTest(){
        setShowSettings(false);
    }

    @Override
    public void simpleInitApp() {
        try{
            CL.create();
            System.out.println("CL initialized");

            Drawable drawable=Display.getDrawable();
            System.out.println("Drawable obtained");

            List<CLPlatform> platforms=CLPlatform.getPlatforms();
            if(platforms==null) {
                System.out.println("/!\\ OpenCL not supported.");
            }else{
                System.out.println(platforms.size()+" platforms available.");
                for(CLPlatform platform:platforms){
                    try{
                        System.out.println();
                        System.out.println("Trying platform: "+platform.getInfoString(CL10.CL_PLATFORM_NAME));
                        List<CLDevice> devices=platform.getDevices(CL10.CL_DEVICE_TYPE_ALL);
                        System.out.println("  Available devices: "+devices.size());

                        for(CLDevice device:devices){
                            try{
                                System.out.println("    Trying device: "+device.getInfoString(CL10.CL_DEVICE_NAME));

                                List<CLDevice> usable_devices=new LinkedList<CLDevice>();
                                usable_devices.add(device); // We want to test 1
                                                            // device per time

                                IntBuffer err=BufferUtils.createIntBuffer(1);
                                CLContext context=CLContext.create(platform,usable_devices,null,drawable,err);
                                Util.checkCLError(err.get());
                                System.out.println("      Context created.");

                                err=BufferUtils.createIntBuffer(1);
                                CLCommandQueue cqueue=CL10.clCreateCommandQueue(context,usable_devices.get(0),CL10.CL_QUEUE_PROFILING_ENABLE,err);
                                Util.checkCLError(err.get());
                                System.out.println("      Command queue created");

                                err=BufferUtils.createIntBuffer(1);

                                FloatBuffer buffer=BufferUtils.createFloatBuffer(10);
                                for(int i=0;i<10;i++)
                                    buffer.put(3*i);
                                buffer.rewind();

                                CLMem clmem=CL10.clCreateBuffer(context,CL10.CL_MEM_READ_WRITE|CL10.CL_MEM_COPY_HOST_PTR,buffer,err);

                                Util.checkCLError(err.get());
                                System.out.println("      R/W Buffer created");

                                err=BufferUtils.createIntBuffer(1);
                                String source="__kernel void main(\n"+"    __global float *buff,\n"+"    int size      ) {\n"+"    unsigned int i = get_global_id(0);\n"+"    if(i<size)buff[i]=pow(buff[i],2);\n"+"}\n";
                                CLProgram program=CL10.clCreateProgramWithSource(context,source,err);
                                Util.checkCLError(err.get());
                                System.out.println("      Compiling program");
                                Util.checkCLError(CL10.clBuildProgram(program,usable_devices.get(0),"",null));
                                System.out.println("      Program compiled");

                                err=BufferUtils.createIntBuffer(1);
                                System.out.println("      Building kernel");
                                CLKernel kernel=CL10.clCreateKernel(program,"main",null);
                                System.out.println("      Setting args");
                                kernel.setArg(0,clmem);
                                kernel.setArg(1,10f);
                                Util.checkCLError(err.get());
                                System.out.println("      Kernel created");

                                PointerBuffer workers=BufferUtils.createPointerBuffer(1);
                                workers.put(0,10);
                                System.out.println("      Workers created");

                                System.out.println("      Starting kernel");
                                CL10.clEnqueueNDRangeKernel(cqueue,kernel,1,null,workers,null,null,null);
                                System.out.println("      Kernel Executed");

                                System.out.println("      Reading results");
                                FloatBuffer rbuffer=BufferUtils.createFloatBuffer(10);
                                CL10.clEnqueueReadBuffer(cqueue,clmem,CL10.CL_TRUE,0,rbuffer,null,null);
                                CL10.clFinish(cqueue);

                                StringBuilder result_stringb=new StringBuilder();
                                for(int i=0;i<10;i++)
                                    result_stringb.append(rbuffer.get(i)).append(" ");

                                StringBuilder expected_stringb=new StringBuilder();
                                for(int i=0;i<10;i++)
                                    expected_stringb.append(FastMath.pow(buffer.get(i),2)).append(" ");

                                String result_string=result_stringb.toString();
                                String expected_string=expected_stringb.toString();
                                System.out.println("      Results:  "+result_string+"\n      Expected: "+expected_string);
                                System.out.println(result_string.equals(expected_string)?"      Status: OK":"      Status: /!\\FAILED");

                                System.out.println("      Releasing memory");
                                CL10.clReleaseMemObject(clmem);
                                CL10.clReleaseKernel(kernel);
                                CL10.clReleaseProgram(program);
                                CL10.clReleaseCommandQueue(cqueue);
                                CL10.clReleaseContext(context);
                                CL.destroy();
                            }catch(Throwable e){
                                System.out.println("      Error using platform "+platform.getInfoString(CL10.CL_PLATFORM_NAME)+" and device "+device.getInfoString(CL10.CL_DEVICE_NAME));
                                e.printStackTrace();
                            }
                        }
                    }catch(Throwable e){
                        System.out.println("  Error using platform "+platform.getInfoString(CL10.CL_PLATFORM_NAME));
                        e.printStackTrace();
                    }
                }
            }
        }catch(Throwable e){
            e.printStackTrace();
        }
        stop();
    }
}
