/**
 * OpenCL program rendering 2D spatial sensitivity plots of the CSL microphone 
 * array.
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */

// set openCL flags for graphic cards
#ifdef cl_khr_fp64
 #pragma OPENCL EXTENSION cl_khr_fp64 : enable
#elif defined(cl_amd_fp64)
 #pragma OPENCL EXTENSION cl_amd_fp64 : enable
#else
 #error "Double precision floating point not supported by OpenCL implementation."
#endif

// -- DSB beamformer sensitivity computation --

__constant int CHANNELS = 64;
__constant double SPEED_OF_SOUND = 343.2;

double getDelayFromMicToPoint
(
  __global double* mics, 
  double           x, 
  double           y, 
  double           z, 
  int              n
)
{
  int i = n*3;
  double u = (mics[i]   - x)*(mics[i]   - x);
  double v = (mics[i+1] - y)*(mics[i+1] - y);
  double w = (mics[i+2] - z)*(mics[i+2] - z);
  return (sqrt(u+v+w)/100)/SPEED_OF_SOUND;
}

double getDbValue
(
  double           x, 
  double           y, 
  double           z, 
  float            freq, 
  __global double* mics,
  __global char*   micsActive, 
  __global float*  steerVec
)
{
  double delta = 0;
  double tau = 0;
  double a_n = 1, w_n = 1;
  double sum = 0, sumRe = 0, sumIm = 0;
  int k = 0;
    
  for (int n = 0; n < CHANNELS; n++) 
  {
    if (micsActive[n] == 0) 
    {
      continue;
    }
    k++;
    tau = getDelayFromMicToPoint(mics,x,y,z,n);
    delta = 2 * M_PI * freq * (steerVec[n] + tau);
    sumRe += w_n * a_n * cos(delta);
    sumIm += w_n * a_n * sin(delta);
  }
  sum = sqrt((sumRe*sumRe) + (sumIm*sumIm)) / k;
  return 20 * log10(sum);
}

double getDbFromPixel
(
  int              x, 
  int              y, 
  int              sliceSelect, 
  int              slicePos, 
  float            freq, 
  __global double* mics, 
  __global char*   micsActive, 
  __global float*  steerVec, 
  int              width, 
  int              height
)
{
  double xPos = 0, yPos = 0, zPos=0;
  if (sliceSelect==1)
  { // XY
    xPos = x - (width/2);
    yPos = -y + (height/2);
    zPos = slicePos;
  } 
  else if (sliceSelect==2)
  { // XZ
    xPos = x - (width/2);
    yPos = slicePos;
    zPos = height-y;
  } 
  else if (sliceSelect==3)
  { // YZ
    xPos = slicePos;
    yPos = x - (width/2);
    zPos = height-y;
  }
  return getDbValue(xPos, yPos, zPos, freq, mics, micsActive, steerVec);
}

// -- Color mapping --

__constant float COL = 50.0/255;
float4 getColor(double dbIn) 
{
  float db = (float)((dbIn>0) ? 0 : ((dbIn< -36) ? -36 : dbIn));
    
  if (db >= -4) 
  {
    db = -db / 4;
    return (float4)(1, db, (1 - db) * COL, 1);
  } 
  else if (db >= -12) 
  {
    db = -(db + 4)/8;
    return (float4)(1 - db, 0.5 + (0.5 * (1 - db)), db, 1);
  } else if (db >= -36) 
  {
    db = 0.5+((db + 12) / 48);
    return (float4)(0, db, db+db , 1);
  }
}

__constant float NORM1 = 255.0f/ 4;
__constant float NORM2 = 255.0f/ 8;
__constant float NORM3 = 255.0f/ 48;

__constant int A_SHIFT = 0;
__constant int R_SHIFT = 8;
__constant int G_SHIFT = 16;
__constant int B_SHIFT = 24;

int getIntColor(double dbIn) 
{
  double db = (dbIn>0) ? 0 : ((dbIn< -36) ? -36 : dbIn);
  if (db >= -4) 
  {
    db = -db*NORM1;
    return 255                 << A_SHIFT
         | 255                 << R_SHIFT
         | (int)(db)           << G_SHIFT
         | (int)((255-db)*COL) << B_SHIFT;
  } 
  else if (db >= -12) 
  {
    db = -(db+4)*NORM2;
   return 255                       << A_SHIFT
        | (int)(255-db)             << R_SHIFT
        | (int)(127.5+0.5*(255-db)) << G_SHIFT
        | (int)(db)                 << B_SHIFT;
  } 
  else if (db >= -36) 
  {
    db = 127.5+(db+12)*NORM3;
    return 255          << A_SHIFT
         |  0           << R_SHIFT
         | (int)(db)    << G_SHIFT
         | (int)(db+db) << B_SHIFT;
  }
}

// -- CL kernel functions --

__kernel void getSensitivityIntArray
(
  float            freq, 
  int              sliceSelect, 
  int              slicePos, 
  int              width, 
  int              height, 
  __global double* mics, 
  __global char*   micsActive, 
  __global float*  steerVec, 
  __global int*    out
) 
{
  int x = get_global_id(0);
  int y = get_global_id(1);
  double db = getDbFromPixel(x,y,sliceSelect,slicePos,freq,mics,micsActive,steerVec,width,height);
  out[x+y*width] = getIntColor(db);
}

__kernel void getSensitivityImage
(
  float                  freq, 
  int                    sliceSelect, 
  int                    slicePos, 
  __global double*       mics, 
  __global char*         micsActive, 
  __global float*        steerVec, 
  __write_only image2d_t outImg
)
{
  int width = get_image_width(outImg);
  int height = get_image_height(outImg);  
  int x = get_global_id(0);
  int y = get_global_id(1);
  double db = getDbFromPixel(x, y, sliceSelect, slicePos, freq, mics, micsActive, steerVec, width, height);
  write_imagef(outImg, (int2){x,y}, getColor(db));
}

// EOF
