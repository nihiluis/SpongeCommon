/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.world.gen.populators;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.MoreObjects;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenIceSpike;
import net.minecraft.world.gen.feature.WorldGenerator;
import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.api.world.gen.PopulatorType;
import org.spongepowered.api.world.gen.PopulatorTypes;
import org.spongepowered.api.world.gen.populator.IceSpike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(WorldGenIceSpike.class)
public abstract class MixinWorldGenIceSpike extends WorldGenerator implements IceSpike {

    private VariableAmount height;
    private VariableAmount increase;
    private VariableAmount count;
    private double prob;

    @Inject(method = "<init>()V", at = @At("RETURN") )
    public void onConstructed(CallbackInfo ci) {
        this.height = VariableAmount.baseWithRandomAddition(7, 4);
        this.increase = VariableAmount.baseWithRandomAddition(10, 30);
        this.prob = 0.0166667D; // 1 in 60
        this.count = VariableAmount.fixed(2);
    }

    @Override
    public PopulatorType getType() {
        return PopulatorTypes.ICE_SPIKE;
    }

    @Override
    public void populate(org.spongepowered.api.world.World worldIn, Extent extent, Random random) {
        Vector3i min = extent.getBlockMin();
        Vector3i size = extent.getBlockSize();
        World world = (World) worldIn;
        int x;
        int z;
        BlockPos chunkPos = new BlockPos(min.getX(), min.getY(), min.getZ());
        int n = this.count.getFlooredAmount(random);
        for (int i = 0; i < n; ++i) {
            x = random.nextInt(size.getX());
            z = random.nextInt(size.getZ());
            generate(world, random, world.getHeight(chunkPos.add(x, 0, z)));
        }
    }

    /**
     * @author Deamon - December 12th, 2015
     * 
     * @reason overwritten to make use of populator parameters
     */
    @Override
    @Overwrite
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        while (worldIn.isAirBlock(position) && position.getY() > 2) {
            position = position.down();
        }

        if (worldIn.getBlockState(position).getBlock() != Blocks.SNOW) {
            return false;
        }
        position = position.up(rand.nextInt(4));
        // Sponge Start
        int height = this.height.getFlooredAmount(rand);
        int width = height / 4 + rand.nextInt(2);
        if (width > 1 && rand.nextDouble() < this.prob) {
            // position.up(10 + rand.nextInt(30));
            position = position.up(this.increase.getFlooredAmount(rand));
        }
        // Sponge End
        if (width > 1 && rand.nextInt(60) == 0) {
            position = position.up(10 + rand.nextInt(30));
        }

        for (int k = 0; k < height; ++k) {
            float f = (1.0F - (float) k / (float) height) * width;
            int l = MathHelper.ceil(f);

            for (int i1 = -l; i1 <= l; ++i1) {
                float f1 = MathHelper.abs(i1) - 0.25F;

                for (int j1 = -l; j1 <= l; ++j1) {
                    float f2 = MathHelper.abs(j1) - 0.25F;

                    if ((i1 == 0 && j1 == 0 || f1 * f1 + f2 * f2 <= f * f)
                            && (i1 != -l && i1 != l && j1 != -l && j1 != l || rand.nextFloat() <= 0.75F)) {
                        IBlockState iblockstate = worldIn.getBlockState(position.add(i1, k, j1));
                        Block block = iblockstate.getBlock();

                        if (iblockstate.getMaterial() == Material.AIR || block == Blocks.DIRT || block == Blocks.SNOW || block == Blocks.ICE) {
                            this.setBlockAndNotifyAdequately(worldIn, position.add(i1, k, j1), Blocks.PACKED_ICE.getDefaultState());
                        }

                        if (k != 0 && l > 1) {
                            iblockstate = worldIn.getBlockState(position.add(i1, -k, j1));
                            block = iblockstate.getBlock();

                            if (iblockstate.getMaterial() == Material.AIR || block == Blocks.DIRT || block == Blocks.SNOW || block == Blocks.ICE) {
                                this.setBlockAndNotifyAdequately(worldIn, position.add(i1, -k, j1), Blocks.PACKED_ICE.getDefaultState());
                            }
                        }
                    }
                }
            }
        }

        int k1 = width - 1;

        if (k1 < 0) {
            k1 = 0;
        } else if (k1 > 1) {
            k1 = 1;
        }

        for (int l1 = -k1; l1 <= k1; ++l1) {
            for (int i2 = -k1; i2 <= k1; ++i2) {
                BlockPos blockpos = position.add(l1, -1, i2);
                int j2 = 50;

                if (Math.abs(l1) == 1 && Math.abs(i2) == 1) {
                    j2 = rand.nextInt(5);
                }

                while (blockpos.getY() > 50) {
                    IBlockState iblockstate1 = worldIn.getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();

                    if (iblockstate1.getMaterial() != Material.AIR && block1 != Blocks.DIRT && block1 != Blocks.SNOW && block1 != Blocks.ICE
                            && block1 != Blocks.PACKED_ICE) {
                        break;
                    }

                    this.setBlockAndNotifyAdequately(worldIn, blockpos, Blocks.PACKED_ICE.getDefaultState());
                    blockpos = blockpos.down();
                    --j2;

                    if (j2 <= 0) {
                        blockpos = blockpos.down(rand.nextInt(5) + 1);
                        j2 = rand.nextInt(5);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public VariableAmount getHeight() {
        return this.height;
    }

    @Override
    public void setHeight(VariableAmount height) {
        this.height = height;
    }

    @Override
    public double getExtremeSpikeProbability() {
        return this.prob;
    }

    @Override
    public void setExtremeSpikeProbability(double p) {
        this.prob = GenericMath.clamp(p, 0, 1);
    }

    @Override
    public VariableAmount getExtremeSpikeIncrease() {
        return this.increase;
    }

    @Override
    public void setExtremeSpikeIncrease(VariableAmount increase) {
        this.increase = increase;
    }

    @Override
    public VariableAmount getSpikesPerChunk() {
        return this.count;
    }

    @Override
    public void setSpikesPerChunk(VariableAmount count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("Type", "IceSpike").add("ExtremeChance", this.prob).add("ExtremeIncrease", this.increase)
                .add("PerChunk", this.count).toString();
    }

}
