#pragma version(1)
#pragma rs java_package_name(com.fantasmo.sdk)
#pragma rs_fp_relaxed

rs_allocation inputImage;
rs_allocation float_input;
rs_allocation tempImageargb;
rs_allocation tempImage;
rs_allocation float_output;
float hScale, vScale;
static uint inputWidth = 0;
static uint inputHeight = 0;
static uint outputWidth = 0;
static uint outputHeight = 0;
float hSupport, vSupport;
rs_allocation hCoeffs, hBounds, vCoeffs, vBounds;
uint hSize, vSize;
uint hOffset, vOffset;
static const float a = -0.5;
static const float3 translate = (float3){0.485, 0.456, 0.406};
static const float3 scale = (float3){0.229, 0.224, 0.225};


static float bicubic_filter(float x) {
    float xx = x;
    if(xx < 0) {
        xx *= -1;
    }
    if (xx < 1.0) {
        return ((a + 2.0) * xx - (a + 3.0)) * xx * xx + 1;
    }
    if (xx < 2.0) {
        return (((xx - 5) * xx + 8) * xx - 4) * a;
    }
    return 0.0;
}

float RS_KERNEL bicubic_hcoeff(float in, uint32_t x, uint32_t y) {
        float center = ((float)x + 0.5) * hScale;
        // Round the value
        int xmin = max((int)(center - hSupport + 0.5), 0);

        return bicubic_filter((y + xmin - center + 0.5f) / hScale) / hScale;
}

float RS_KERNEL bicubic_vcoeff(float in, uint32_t x, uint32_t y) {
        float center = ((float)x + 0.5) * vScale;
        // Round the value
        int xmin = max((int)(center - vSupport + 0.5), 0);

        return bicubic_filter((y + xmin - center + 0.5f) / vScale) / vScale;
}

uint2 RS_KERNEL bicubic_hbounds(uint2 in, uint32_t x) {
        float center = ((float)x + 0.5) * hScale;
        // Round the value
        uint xmin = (uint)(center - hSupport + 0.5);

        uint xmax = min((uint)(center + hSupport + 0.5), inputWidth);

        return (uint2){xmin, xmax};
}

uint2 RS_KERNEL bicubic_vbounds(uint2 in, uint32_t x) {
        float center = ((float)x + 0.5) * vScale;
        // Round the value
        uint ymin = (uint)(center - vSupport + 0.5);

        uint ymax = min((uint)(center + vSupport + 0.5), inputHeight);

        return (uint2){ymin, ymax};
}
float4 RS_KERNEL bicubic_h(uchar4 in, uint x, uint y) {
    int2 hBound = rsGetElementAt_int2(hBounds, x);
    float4 result;
    for(uint inx = hBound.s0; inx < hBound.s1; inx++) {
        result += rsUnpackColor8888(rsGetElementAt_uchar4(inputImage, inx, y)) * rsGetElementAt_float(hCoeffs, x, inx - hBound.s0);
    }
    return result;
}

float4 RS_KERNEL bicubic_v(float4 in, uint x, uint y) {
    int2 vBound = rsGetElementAt_int2(vBounds, y);
    float4 result;
    for(uint iny = vBound.s0; iny < vBound.s1; iny++) {
        result += rsGetElementAt_float4(tempImage, x, iny) * rsGetElementAt_float(vCoeffs, y, iny - vBound.s0);
    }
    return result;
}

uchar4 RS_KERNEL bicubic_v_pack(float4 in, uint x, uint y) {
    int2 vBound = rsGetElementAt_int2(vBounds, y);
    float4 result;
    for(uint iny = vBound.s0; iny < vBound.s1; iny++) {
        result += rsGetElementAt_float4(tempImage, x, iny) * rsGetElementAt_float(vCoeffs, y, iny - vBound.s0);
    }
    return rsPackColorTo8888(result);
}

float RS_KERNEL populate_tensor(float in, uint x, uint y) {
    uint rgb = y / outputHeight;
    return (rsGetElementAt_float4(float_output, x, y - rgb * outputHeight)[rgb] - translate[rgb]) / scale[rgb];
}

void resize(rs_allocation output) {
    uint inputX = rsAllocationGetDimX(inputImage);
    uint inputY = rsAllocationGetDimY(inputImage);
    uint outputX = rsAllocationGetDimX(output);
    uint outputY = rsAllocationGetDimY(output) / 3;

    // Seeing if input has a new size
    bool newInput = inputX != inputWidth || inputY != inputHeight;
     // Seeing if output has a new size
    bool newOutput = outputX != outputWidth || outputY != outputHeight;

    if(newInput) {
        // Allocating new inputs
        inputWidth = inputX;
        inputHeight = inputY;
        float_input = rsCreateAllocation_float4(inputWidth, inputHeight);
    }
    if(newOutput) {
        outputWidth = outputX;
        outputHeight = outputY;
        float_output = rsCreateAllocation_float4(outputWidth, outputHeight);
    }

    if(newInput || newOutput) {
        tempImage = rsCreateAllocation_float4(outputWidth, inputHeight);

        hScale = (float)inputWidth / (float)outputWidth;
        hSupport = 2. * hScale;
        hSize = 2 * (uint32_t)ceil(hSupport) + 1;

        hCoeffs = rsCreateAllocation_float(outputWidth, hSize);
        hBounds = rsCreateAllocation_uint2(outputWidth);
        vScale = (float)inputHeight / (float)outputHeight;
        vSupport = 2. * vScale;
        vSize = 2 * (uint32_t)ceil(vSupport) + 1;

        vCoeffs = rsCreateAllocation_float(outputHeight, vSize);
        vBounds = rsCreateAllocation_uint2(outputHeight);
        rsForEach(bicubic_hcoeff, hCoeffs, hCoeffs);
        rsForEach(bicubic_vcoeff, vCoeffs, vCoeffs);
        rsForEach(bicubic_hbounds, hBounds, hBounds);
        rsForEach(bicubic_vbounds, vBounds, vBounds);
    }

    rsForEach(bicubic_h, tempImage, tempImage);
    rsForEach(bicubic_v_pack, output, output);
}

void make_tf_tensor(rs_allocation output) {
    uint inputX = rsAllocationGetDimX(inputImage);
    uint inputY = rsAllocationGetDimY(inputImage);
    uint outputX = rsAllocationGetDimX(output);
    uint outputY = rsAllocationGetDimY(output) / 3;

    // Seeing if input has a new size
    bool newInput = inputX != inputWidth || inputY != inputHeight;
     // Seeing if output has a new size
    bool newOutput = outputX != outputWidth || outputY != outputHeight;

    if(newInput) {
        // Allocating new inputs
        inputWidth = inputX;
        inputHeight = inputY;
        float_input = rsCreateAllocation_float4(inputWidth, inputHeight);
    }
    if(newOutput) {
        outputWidth = outputX;
        outputHeight = outputY;
        float_output = rsCreateAllocation_float4(outputWidth, outputHeight);
    }

    if(newInput || newOutput) {
        tempImage = rsCreateAllocation_float4(outputWidth, inputHeight);

        hScale = (float)inputWidth / (float)outputWidth;
        hSupport = 2. * hScale;
        hSize = 2 * (uint32_t)ceil(hSupport) + 1;

        hCoeffs = rsCreateAllocation_float(outputWidth, hSize);
        hBounds = rsCreateAllocation_uint2(outputWidth);
        vScale = (float)inputHeight / (float)outputHeight;
        vSupport = 2. * vScale;
        vSize = 2 * (uint32_t)ceil(vSupport) + 1;

        vCoeffs = rsCreateAllocation_float(outputHeight, vSize);
        vBounds = rsCreateAllocation_uint2(outputHeight);
        rsForEach(bicubic_hcoeff, hCoeffs, hCoeffs);
        rsForEach(bicubic_vcoeff, vCoeffs, vCoeffs);
        rsForEach(bicubic_hbounds, hBounds, hBounds);
        rsForEach(bicubic_vbounds, vBounds, vBounds);
    }

    rsForEach(bicubic_h, tempImage, tempImage);
    rsForEach(bicubic_v, float_output, float_output);
    rsForEach(populate_tensor, output, output);
}