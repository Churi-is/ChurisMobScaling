package net.churismobscaling.util;

import java.util.Random;

/**
 * OpenSimplex Noise implementation for 2D noise generation.
 * <p>
 * This implementation is based on the work by Kurt Spencer and has been
 * adapted for use in the Churis Mob Scaling mod.
 */
public class OpenSimplexNoise {
    private static final double STRETCH_CONSTANT_2D = -0.211324865405187;    // (1/Math.sqrt(2+1)-1)/2
    private static final double SQUISH_CONSTANT_2D = 0.366025403784439;      // (Math.sqrt(2+1)-1)/2
    
    // Normalization constant, empirically derived to scale output roughly to [-1, 1].
    private static final double NORM_CONSTANT_2D = 47;

    private static final int PERMUTATION_TABLE_SIZE = 256;
    private static final long LCG_MULTIPLIER = 6364136223846793005L;
    private static final long LCG_INCREMENT = 1442695040888963407L;
    private static final int SEED_SHUFFLE_OFFSET = 31; // Offset used in random index calculation during shuffling.
    private static final int BYTE_MASK = 0xFF; // Mask to get the last 8 bits of an integer.
    // Mask to ensure an even index (0, 2, ..., 14) for accessing gradient pairs.
    private static final int GRADIENT_PAIR_MASK = 0x0E;
    // Value R^2 used in attenuation formula: (R^2 - distSq)^4. Here R^2 = 2.0.
    private static final double ATTENUATION_R_SQUARED = 2.0;
    
    private final short[] perm;
    
    // Gradients for 2D. Each pair (dx, dy) defines a gradient vector.
    private static final byte[] GRADIENTS_2D = new byte[] {
            5,  2,    2,  5,
           -5,  2,   -2,  5,
            5, -2,    2, -5,
           -5, -2,   -2, -5,
    };
    
    /**
     * Creates a new OpenSimplexNoise instance with a random seed.
     */
    public OpenSimplexNoise() {
        this(new Random().nextLong());
    }
    
    /**
     * Creates a new OpenSimplexNoise instance with the specified seed.
     *
     * @param seed The seed for the noise generator.
     */
    public OpenSimplexNoise(long seed) {
        perm = new short[PERMUTATION_TABLE_SIZE];
        short[] source = new short[PERMUTATION_TABLE_SIZE];
        for (short i = 0; i < PERMUTATION_TABLE_SIZE; i++) {
            source[i] = i;
        }
        
        // LCG seeding steps to thoroughly mix the initial seed.
        seed = seed * LCG_MULTIPLIER + LCG_INCREMENT;
        seed = seed * LCG_MULTIPLIER + LCG_INCREMENT;
        seed = seed * LCG_MULTIPLIER + LCG_INCREMENT;
        
        // Fisher-Yates shuffle to create the permutation table.
        for (int i = PERMUTATION_TABLE_SIZE - 1; i >= 0; i--) {
            seed = seed * LCG_MULTIPLIER + LCG_INCREMENT;
            int r = (int)((seed + SEED_SHUFFLE_OFFSET) % (i + 1));
            if (r < 0) {
                r += (i + 1);
            }
            perm[i] = source[r];
            source[r] = source[i]; // Move the chosen element out of the source pool.
        }
    }
    
    /**
     * Samples the 2D OpenSimplex noise field at the given coordinates.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The noise value, normalized to be roughly within the range [-1, 1].
     */
    public double sample(double x, double y) {
        // Place input coordinates onto grid.
        double stretchOffset = (x + y) * STRETCH_CONSTANT_2D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        
        // Floor to get grid coordinates of rhombus (stretched square) super-cell origin.
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        
        // Skew out to get actual coordinates of rhombus origin. We'll need these later.
        double squishOffset = (xsb + ysb) * SQUISH_CONSTANT_2D;
        double xb = xsb + squishOffset;
        double yb = ysb + squishOffset;
        
        // Compute grid coordinates relative to rhombus origin.
        double xins = xs - xsb;
        double yins = ys - ysb;
        
        // Sum those together to get a value that determines which region we're in.
        double inSum = xins + yins;

        // Positions relative to origin point.
        double dx0 = x - xb;
        double dy0 = y - yb;
        
        // We'll be defining these inside the next block and using them afterwards.
        double dx_ext, dy_ext;
        int xsv_ext, ysv_ext;
        
        double value = 0;
        
        // Contribution (1,0)
        double dx1 = dx0 - 1 - SQUISH_CONSTANT_2D;
        double dy1 = dy0 - 0 - SQUISH_CONSTANT_2D;
        double attn1 = ATTENUATION_R_SQUARED - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0) {
            attn1 *= attn1;
            value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, dx1, dy1);
        }
        
        // Contribution (0,1)
        double dx2 = dx0 - 0 - SQUISH_CONSTANT_2D;
        double dy2 = dy0 - 1 - SQUISH_CONSTANT_2D;
        double attn2 = ATTENUATION_R_SQUARED - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0) {
            attn2 *= attn2;
            value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, dx2, dy2);
        }
        
        // Determine which simplex we are in and calculate the contribution from the third vertex.
        if (inSum <= 1) { // We're inside the triangle (2-Simplex) at (0,0)
            double zins = 1 - inSum;
            if (zins > xins || zins > yins) { // (0,0) is one of the closest two triangular vertices
                if (xins > yins) {
                    xsv_ext = xsb + 1;
                    ysv_ext = ysb - 1;
                    dx_ext = dx0 - 1;
                    dy_ext = dy0 + 1;
                } else {
                    xsv_ext = xsb - 1;
                    ysv_ext = ysb + 1;
                    dx_ext = dx0 + 1;
                    dy_ext = dy0 - 1;
                }
            } else { // (1,0) and (0,1) are the closest two vertices.
                xsv_ext = xsb + 1;
                ysv_ext = ysb + 1;
                dx_ext = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
                dy_ext = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
            }
        } else { // We're inside the triangle (2-Simplex) at (1,1)
            double zins = 2 - inSum;
            if (zins < xins || zins < yins) { // (1,1) is one of the closest two triangular vertices
                if (xins > yins) {
                    xsv_ext = xsb + 2; // (2,0) relative to simplex origin
                    ysv_ext = ysb + 0;
                    dx_ext = dx0 - 2 - 2 * SQUISH_CONSTANT_2D;
                    dy_ext = dy0 + 0 - 2 * SQUISH_CONSTANT_2D;
                } else {
                    xsv_ext = xsb + 0; // (0,2) relative to simplex origin
                    ysv_ext = ysb + 2;
                    dx_ext = dx0 + 0 - 2 * SQUISH_CONSTANT_2D;
                    dy_ext = dy0 - 2 - 2 * SQUISH_CONSTANT_2D;
                }
            } else { // (0,1) and (1,0) are the closest two vertices. This case needs to be relative to (1,1)
                dx_ext = dx0; // This was originally the point (0,0) relative to the main origin, now (0,0) relative to (1,1) origin
                dy_ext = dy0;
                xsv_ext = xsb; // This was xsb, relative to (0,0) origin. Now it's the (0,0) point of the other simplex.
                ysv_ext = ysb;
            }
            // Shift main origin to (1,1) for the current simplex
            xsb += 1;
            ysb += 1;
            dx0 = dx0 - 1 - 2 * SQUISH_CONSTANT_2D; // Recalculate dx0, dy0 relative to the new origin (1,1)
            dy0 = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
        }
        
        // Contribution (0,0) or (1,1)
        double attn0 = ATTENUATION_R_SQUARED - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0) {
            attn0 *= attn0;
            value += attn0 * attn0 * extrapolate(xsb, ysb, dx0, dy0);
        }
        
        // Extra Vertex
        double attn_ext = ATTENUATION_R_SQUARED - dx_ext * dx_ext - dy_ext * dy_ext;
        if (attn_ext > 0) {
            attn_ext *= attn_ext;
            value += attn_ext * attn_ext * extrapolate(xsv_ext, ysv_ext, dx_ext, dy_ext);
        }
        
        return value / NORM_CONSTANT_2D;
    }
    
    /**
     * Calculates the contribution from a specific grid corner using its gradient.
     * The gradient is selected from {@link #GRADIENTS_2D} based on the permuted hash of coordinates.
     *
     * @param xsb The integer x-coordinate of the grid corner.
     * @param ysb The integer y-coordinate of the grid corner.
     * @param dx The x-distance from the point to the grid corner.
     * @param dy The y-distance from the point to the grid corner.
     * @return The extrapolated gradient value.
     */
    private double extrapolate(int xsb, int ysb, double dx, double dy) {
        // Combine permuted xsb with ysb, then use the result to index into perm again.
        // The final value is masked to select a gradient pair.
        int p1 = perm[xsb & BYTE_MASK];
        int p2 = perm[(p1 + ysb) & BYTE_MASK];
        int index = p2 & GRADIENT_PAIR_MASK; 
        return GRADIENTS_2D[index] * dx + GRADIENTS_2D[index + 1] * dy;
    }
    
    /**
     * A faster alternative to {@link Math#floor(double)}.
     * This method is faster because it uses type casting and a conditional check
     * instead of a more general (and slower) native method call.
     *
     * @param x The value to floor.
     * @return The largest integer less than or equal to x.
     */
    private static int fastFloor(double x) {
        int xi = (int)x;
        return x < xi ? xi - 1 : xi;
    }
}
