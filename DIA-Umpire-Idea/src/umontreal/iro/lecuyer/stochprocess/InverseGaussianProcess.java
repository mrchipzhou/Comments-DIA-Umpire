

/*
 * Class:        InverseGaussianProcess
 * Description:  
 * Environment:  Java
 * Software:     SSJ 
 * Copyright (C) 2001  Pierre L'Ecuyer and Universite de Montreal
 * Organization: DIRO, Universite de Montreal
 * @author       
 * @since

 * SSJ is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or
 * any later version.

 * SSJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * A copy of the GNU General Public License is available at
   <a href="http://www.gnu.org/licenses">GPL licence site</a>.
 */

package umontreal.iro.lecuyer.stochprocess;
import umontreal.iro.lecuyer.rng.*;
import umontreal.iro.lecuyer.probdist.*;
import umontreal.iro.lecuyer.randvar.*;



/**
 * The inverse Gaussian process is a non-decreasing process
 * where the increments are additive and are given by the
 * inverse gaussian distribution,
 * {@link umontreal.iro.lecuyer.probdist.InverseGaussianDist InverseGaussianDist}.
 * With parameters <SPAN CLASS="MATH"><I>&#948;</I></SPAN> and <SPAN CLASS="MATH"><I>&#947;</I></SPAN>, the
 * time increments are given by
 * {@link umontreal.iro.lecuyer.probdist.InverseGaussianDist InverseGaussianDist}
 * <SPAN CLASS="MATH">(<I>&#948;dt</I>/<I>&#947;</I>, <I>&#948;</I><SUP>2</SUP><I>dt</I><SUP>2</SUP>)</SPAN>.
 * 
 * <P>
 * [We here use the inverse gaussian distribution
 * parametrized with IGDist
 * <SPAN CLASS="MATH">(<I>&#956;</I>, <I>&#955;</I>)</SPAN>, where 
 * <SPAN CLASS="MATH"><I>&#956;</I> = <I>&#948;</I>/<I>&#947;</I></SPAN>
 * and 
 * <SPAN CLASS="MATH"><I>&#955;</I> = <I>&#948;</I><SUP>2</SUP></SPAN>.  If we instead used the parametrization
 * 
 * <SPAN CLASS="MATH">IGDist<SUP>1#1</SUP>(<I>&#948;</I>, <I>&#947;</I>)</SPAN>,
 * then the increment distribution of our process would have been written
 * more simply as 
 * <SPAN CLASS="MATH">IGDist<SUP>[tex2html_wrap_inline160]</SUP>(<I>&#948;dt</I>, <I>&#947;</I>)</SPAN>.]
 * 
 * <P>
 * The increments are generated by using
 * the inversion of the cumulative distribution function.
 * It therefore uses only one {@link umontreal.iro.lecuyer.rng.RandomStream RandomStream}.
 * Subclasses of this class use different generating methods and some need
 * two {@link umontreal.iro.lecuyer.rng.RandomStream RandomStream}'s.
 * 
 * <P>
 * The initial value of this process is the initial observation time.
 * 
 */
public class InverseGaussianProcess extends StochasticProcess  {

    protected RandomStream       stream;

    protected double   delta;
    protected double   gamma;

    protected double   deltaOverGamma;
    protected double   deltaSquare;
    // mu and lambda are the common names of the params for InverseGaussianGen.
    protected double[] imu;
    protected double[] ilam;

    // Number of random streams needed by the current class
    // to generate an IG process.  For this class, = 1, for subclasses
    // will sometimes be, = 2.
    int numberOfRandomStreams;



    // needed by InverseGaussianProcessMSH
   protected InverseGaussianProcess()  { }


   /**
    * Constructs a new <TT>InverseGaussianProcess</TT>.
    * The initial value <SPAN CLASS="MATH"><I>s</I>0</SPAN> will be overridden by <SPAN CLASS="MATH"><I>t</I>[0]</SPAN> when
    * the observation times are set.
    * 
    */
   public InverseGaussianProcess (double s0, double delta, double gamma,
                                  RandomStream stream)  {
        this.x0 = s0;
        setParams(delta, gamma);
        this.stream = stream;
        numberOfRandomStreams = 1;
    }


   public double[] generatePath() {
        double s = x0;
        for (int i = 0; i < d; i++) {
            s += InverseGaussianGen.nextDouble(stream, imu[i], ilam[i]);
            path[i+1] = s;
        }
        observationIndex   = d;
        observationCounter = d;
        return path;
    }

   /**
    * Instead of using the internal stream to generate the path,
    * uses an array of uniforms <SPAN CLASS="MATH"><I>U</I>[0, 1)</SPAN>.  The array should be
    * of the length of the number of periods in the observation
    * times. This method is useful for {@link NormalInverseGaussianProcess}.
    * 
    */
   public double[] generatePath (double[] uniforms01)  {
        double s = x0;
        for (int i = 0; i < d; i++) {
            s += InverseGaussianDist.inverseF(imu[i], ilam[i], uniforms01[i]);
            path[i+1] = s;
        }
        observationIndex   = d;
        observationCounter = d;
        return path;
    }


   /**
    * This method does not work for this class, but will
    * be useful for the subclasses that require two streams.
    * 
    */
   public double[] generatePath (double[] uniforms01, double[] uniforms01b)  {
       throw new UnsupportedOperationException("Use generatePath with 1 stream.");
    }


   public double nextObservation () {
        double s = path[observationIndex];
        s += InverseGaussianGen.nextDouble(stream,
                  imu[observationIndex], ilam[observationIndex]);
        observationIndex++;
        observationCounter = observationIndex;
        path[observationIndex] = s;
        return s;
    }

   /**
    * Sets the parameters.
    * 
    */
   public void setParams (double delta, double gamma)  {
        this.delta = delta;
        this.gamma = gamma;
        deltaOverGamma = delta/gamma;
        deltaSquare    = delta*delta;
        init();
    }


   /**
    * Returns <SPAN CLASS="MATH"><I>&#948;</I></SPAN>.
    * 
    */
   public double getDelta()  {
        return delta;
    }


   /**
    * Returns <SPAN CLASS="MATH"><I>&#947;</I></SPAN>.
    * 
    */
   public double getGamma()  {
        return gamma;
    }


   /**
    * Returns the analytic average which is 
    * <SPAN CLASS="MATH"><I>&#948;t</I>/<I>&#947;</I></SPAN>,
    *    with <SPAN CLASS="MATH"><I>t</I> =</SPAN> <TT>time</TT>.
    * 
    */
   public double getAnalyticAverage (double time)  {
        return delta*time/gamma;
    }


   /**
    * Returns the analytic variance which is 
    * <SPAN CLASS="MATH">(<I>&#948;t</I>)<SUP>2</SUP></SPAN>,
    *    with <SPAN CLASS="MATH"><I>t</I> =</SPAN> <TT>time</TT>.
    * 
    */
   public double getAnalyticVariance (double time)  {
        return delta*delta*time*time;
    }


    protected void init () {
        super.init(); // set path[0] to s0
        if(observationTimesSet){
            x0 = t[0];
            path[0] = x0;
            imu  = new double[d];
            ilam = new double[d];
            for (int j = 0; j < d; j++)
            {
                double temp = delta * (t[j+1] - t[j]);
                imu[j]  = temp / gamma;
                ilam[j] = temp * temp;
            }
        }
    }

   public RandomStream getStream () {
        // It is assumed that stream is always the same
        // as the stream in the InverseGaussianGen.
        return stream;
    }

   public void setStream (RandomStream stream) {
       this.stream = stream;
    }

   /**
    * Returns the number of random streams of this process.
    * It is useful because some subclasses use different number
    * of streams.  It returns 1 for {@link InverseGaussianProcess}.
    * 
    */
   public int getNumberOfRandomStreams()  {
        return numberOfRandomStreams;
    }


}

