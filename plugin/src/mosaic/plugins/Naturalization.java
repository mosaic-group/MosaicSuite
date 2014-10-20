package mosaic.plugins;

import java.util.Vector;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.FloatType;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

class Naturalization implements PlugInFilter
{
	int N_Lap = 2041;
	int Lap_Offset = 1020;


	int N_Grad = 512;
	int Grad_Offset = 256;

	int Pad_SizeR = 3;
	int Pad_SizeC = 3;
	
	float T2_pr[] = {0.2421f ,0.2550f,0.2474f};
	
	<T> void Naturalization(ImagePlus img)
	{
		final ImgFactory< FloatType > imgFactoryF = new ArrayImgFactory< FloatType >( );
		final ImgFactory< IntegerType > imgFactoryI = new ArrayImgFactory< IntegerType >( );
		
		// Image Opener
		
		ImageOpener img = new ImageOpener();
		
		Vector<Float> Theta = new Vector<Float>();
		Theta.add(0.0f);
		Theta.add(0.5f);
		Theta.add(1.0f);
		
		// Image
		
		Img<T> image_orig = ImagePlusAdapter.wrap( img );
		
		// Calculate mean intensity
		double mean_orig = mean();
		
		Img<T> field_R = ImagePlusAdapter.wrap( img );
		Img<T> field_G = ImagePlusAdapter.wrap( img );
		Img<T> field_B = ImagePlusAdapter.wrap( img );
		
		Img<IntegerType> LapCDF = 3,N_Lap,CV_Type;
		Img<IntegerType> GradCDF = 6,N_Grad,CV_Type;		
	}
	
	@Override
	public int setup(String arg, ImagePlus imp) 
	{
		//constant
/*		#define PI 3.1415926
		#define EPS 0.0001
		#define T1_pr 0.3754*/
		//static const DataType T2_pr[] = {0.1446, 0.1446, 0.1446};

		    //get the mean
		    Scalar mean_orig = mean(image_orig);
		    //save the result
		    vector<Mat> image_result(Theta.size());
		    for (int i = 0; i < Theta.size(); ++i)
		    {
		        image_result[i] = Mat::zeros(image_orig.rows,image_orig.cols,CV_8UC3);
		    }

		    copyMakeBorder(image_orig, image, Pad_SizeR, Pad_SizeR, Pad_SizeC, Pad_SizeC, BORDER_REPLICATE);

		    //for reconstruction from Laplace field
		    Mat field_R = Mat::zeros(image.rows,image.cols,CV_Type);
		    Mat field_G = Mat::zeros(image.rows,image.cols,CV_Type);
		    Mat field_B = Mat::zeros(image.rows,image.cols,CV_Type);

		    Mat Fields[3]={field_B,field_G,field_R};


		    vector<Mat> channels(3);
		    split(image, channels);

		    Mat LapCDF = Mat::zeros(3,N_Lap,CV_Type);
		    Mat GradCDF = Mat::zeros(6,N_Grad,CV_Type);

		     //PDF
		     for(int i=0;i<3;i++)
		     {
		            div(channels[i],Fields[i],i,GradCDF,LapCDF);
		     }
		    
		     float T1[] = new float[3];
		     float T2[] = new float[3];
		     float T_tmp;
		     for (int i = 0; i < 3; ++i)
		     {
		         T_tmp = FindT(GradCDF.ptr<DataType>(i), N_Grad, Grad_Offset, EPS);
		         T_tmp += FindT(GradCDF.ptr<DataType>(i+3), N_Grad, Grad_Offset, EPS);
		         T1[i] = T_tmp/(2*T1_pr);
		     }

		     for (int i = 0; i < 3; ++i)
		     {
		         T2[i] = FindT(LapCDF.ptr<DataType>(i), N_Lap, Lap_Offset, EPS)/T2_pr[i];
		     }

		     //change Theta to Nf
		     Mat Nf(Theta.size(),3,CV_Type);
		     for (unsigned int i = 0; i < Theta.size(); ++i)
		     {
		        for (int j = 0; j < 3; ++j)
		        {
		            Nf.at<DataType>(i,j) = (1-Theta[i])*T1[j] + Theta[i]*T2[j];
		        }
		     }

		     //one channle with original size
		     Mat onechannel(image_orig.rows,image_orig.cols,CV_8UC1);
		    //Poisson
		     Tstart = clock();
		    Poisson(Fields, Nf, mean_orig, onechannel, image_result);
		    Tend = clock() - Tstart;
		    cout<<"Time(solver): "<<DataType(Tend)/CLOCKS_PER_SEC<<endl;
		    
		    cout<<"Speed(solver): "<<CLOCKS_PER_SEC/DataType(Tend*1000000.0)*Theta.size()*image_orig.rows * image_orig.cols<<" MPixel/Sec"<<endl;

		    namedWindow( "Original", 1 );
		    imshow( "Original", image_orig);

		    int Half_h = image_orig.rows/2;
		    int Half_w = image_orig.cols/2;
		    int Mid    = Theta.size()/2;

		    Vec3b* p_image;
		    Vec3b* p_result;
		    for (int i = 0; i < image_orig.rows; ++i)
		    {
		        p_image = image_orig.ptr<Vec3b>(i);
		        p_result = image_result[Mid].ptr<Vec3b>(i);
		        for (int j = 0; j < image_orig.cols; ++j)
		        {
		            if ( j >  Half_h - i + Half_w)
		            {
		                if (j <= Half_h - i + Half_w + 3)
		                {
		                    p_image[j].val[0] = 0;
		                    p_image[j].val[1] = 0;
		                    p_image[j].val[2] = 0;
		                }else p_image[j] = p_result[j];
		            }
		        }
		    }

		    namedWindow( "Original/Naturalized(Theta=0.5)", 1 );
		    imshow( "Original/Naturalized(Theta=0.5)", image_orig);

		    namedWindow( "Naturalized", 1 );
		    imshow( "Naturalized", image_result[Mid]);

		    //imwrite(save_name,image_result[0]);

		    return 0;
	}

		double FindT_Evalue(float[] data, int N, int offset, DataType T)
		{
		    double error = 0;

		    double tmp;
		    float[] p_d = data;

		    for (int i=-offset; i<N-offset; ++i) {
		        tmp = atan(T*(i)) - (*p_d++);
		        error += (tmp*tmp);
		    }

		    return error;
		}

		double FindT(float[] data, int N, int OffSet, float eps)
		{
		    //find the best parameter between data and model atan(Tx)/pi+0.5
		    float left; 
		    float right;

		    float m1;
		    float m2;

		    float tmpData[] = new float[N];
		    float p_t[] = tmpData;
		    float p_d[] = data;
		    for (int i = 0; i < N; ++i)
		    {
		        (p_t[i]) = (p_d[i] - 0.5)*Math.PI;
		    }
		    while(right-left>=eps)
		    {
		        m1=left+(right-left)/3;
		        m2=right-(right-left)/3;
		        if(FindT_Evalue(tmpData, N, OffSet, m1) <=FindT_Evalue(tmpData, N, OffSet, m2))
		            right=m2;
		        else
		            left=m1;
		    }
		    return (m1+m2)/2;
		}

		    //process signel channel uint8 image
		<T> void div(Img<T> image, Img<T> field, int ch, Img<T> GradCDF, Img<T> LapPDF)
		{
		    unsigned char * p_row ;
		    float * p_row_field;
		    unsigned char * pnext_row;
		    unsigned char * ppre_row;

		    DataType * Gradx = GradCDF.ptr<DataType>(ch);
		    DataType * Grady = GradCDF.ptr<DataType>(ch+3);

		    DataType * Lap = LapPDF.ptr<DataType>(ch);

		    //very careful here, do not need pad and boarder
		    double f = 1.0/((image.rows-2 -2*Pad_SizeR)*(image.cols-2 - 2*Pad_SizeC));

		    Mat Grad2D = Mat::zeros(N_Grad,N_Grad,CV_Type);


		    short int tmp,indexX, indexY;

		    //not efficient but safe way
		    for(int i = Pad_SizeR + 1; i < image.rows - Pad_SizeR - 1; i++)
				{
		        p_row = image.ptr<unsigned char>(i);
		        p_row_field = field.ptr<DataType>(i);
		        pnext_row = image.ptr<unsigned char>(i+1);
		        ppre_row = image.ptr<unsigned char>(i-1);
		        for(int j = Pad_SizeC + 1; j < image.cols - Pad_SizeC - 1; j++)
				    {
		            tmp = ppre_row[j] + pnext_row[j] + p_row[j-1] + p_row[j+1]-4*p_row[j];
		            p_row_field[j] = (DataType)tmp;
		            tmp += Lap_Offset;
		            Lap[tmp] += f;

		            indexX = Grad_Offset + p_row[j+1] - p_row[j];
		            indexY = Grad_Offset + pnext_row[j] - p_row[j];
		            
		            Grad2D.at<DataType>(indexX, indexY)+= f;
				    }
				}
		    //convert Grad2D into CDF
		    DataType * p_Grad2D;
		    DataType * p_Grad2DNext;
		    //row integration
		    for (int j = 0; j < N_Grad; ++j)
		    {
		        p_Grad2D = Grad2D.ptr<DataType>(j);
		        for (int i = 1; i < N_Grad; ++i)
		        {
		            p_Grad2D[i] += p_Grad2D[i-1];
		        }
		    }
		    //col integration
		    for (int j = 1; j < N_Grad; ++j)
		    {
		        p_Grad2D = Grad2D.ptr<DataType>(j-1);
		        p_Grad2DNext = Grad2D.ptr<DataType>(j);
		        for (int i = 0; i < N_Grad; ++i)
		        {
		            p_Grad2DNext[i] += p_Grad2D[i];
		        }
		    }
		    
		    for (int i = 0; i < N_Grad; ++i)
		    {
		        p_Grad2D = Grad2D.ptr<DataType>(i);
		        for (int j = 0; j < N_Grad; ++j)
		        {
		            *Gradx += *p_Grad2D++;
		        }
		        Gradx++;
		    }

		    for (int i = 0; i < N_Grad; ++i)
		    {
		        p_Grad2D = Grad2D.ptr<DataType>(i);
		        for (int j = 0; j < N_Grad; ++j)
		        {
		            Grady[j] += p_Grad2D[j];
		        }
		    }

		    //scale 
		    Gradx = GradCDF.ptr<DataType>(ch);
		    Grady = GradCDF.ptr<DataType>(ch+3);
		    for (int i = 0; i < N_Grad; ++i)
		    {
		        Gradx[i] /= 255;
		        Grady[i] /= 255;
		    }

		    //convert Lap to CDF
		    for (int i = 1; i < N_Lap; ++i)
		    {
		        Lap[i] += Lap[i-1];
		    }
		}


		void Poisson(Mat* Fields, Mat& Nf, Scalar mean_orig, Mat& channels, vector<Mat>& result)
		{
		    //the input Fields need to be remapped with Nf on each channel
		    //the result is saved to result

		    DataType * p_row;

		    const int M = Fields[0].rows;
		    const int N = Fields[0].cols;
		    const int Total = (M-2)*(N-2);
		    const DataType normalization = 1.0/(4*M*N);

		    //fill the dominator for DST
		    float[] The_D = new DataType[Total];

		    double tmp;
		    p_row = The_D;
		    double tmp_r = Math.PI/(2*M-2);
		    double tmp_c = Math.PI/(2*N-2);
		    double tmp_d;
		    
		    for (int i = 1; i < M-1; ++i)
		    {
		        tmp_d = sin(i*tmp_r);
		        tmp = tmp_d * tmp_d;
		        for (int j = 1; j < N-1; ++j)
		        {
		            tmp_d = sin(j*tmp_c);
		        
		            p_row[i] = 1.0/(4*(tmp + tmp_d*tmp_d)) ;
		        }
		    }
		    
		    //****************do the work here *************//
		    float[] MappedField = new float[Total];
		    float[] OrigField = new float[Total];

		    float[] p_Orig = null;
		    float[] p_MF = null; 

		    fftwf_plan dct_fw, dct_bw;

		    DataType* data_dct = new DataType[Total];
		    
		    Vec3b * p_result;

		    for (int i = 0; i < 3; ++i)
		    {
		        //copy to orig field
		        p_Orig = OrigField;
		        for (int k = 1; k < M-1; ++k)
		        {
		            p_row = Fields[i].ptr<DataType>(k);
		            for (int s = 1; s < N-1; ++s)
		            {
		                *p_Orig++ = p_row[s];
		            }

		        }

		        //process this channel
		        for (int j = 0; j < Nf.rows; ++j)
		        {
		            //map the field
		            p_MF = MappedField;
		            p_Orig = OrigField;
		            DataType scale = Nf.at<DataType>(j,i);
		            for (int s = 0; s < Total; ++s)
		            {
		                (*p_MF++) = -(*p_Orig++)*scale;
		            }

		            //DST(DCT)
		            dct_fw = fftwf_plan_r2r_2d(M-2, N-2,MappedField, data_dct,FFTW_RODFT00, FFTW_RODFT00,FFTW_ESTIMATE | FFTW_DESTROY_INPUT);
		            //dct_fw = fftwf_plan_r2r_2d(M-2, N-2,MappedField, data_dct,FFTW_REDFT10, FFTW_REDFT10,FFTW_ESTIMATE | FFTW_DESTROY_INPUT);
		            fftwf_execute(dct_fw);

		            //divide
		            p_row = The_D;
		            p_MF = data_dct;
		            for (int s = 0; s < Total; ++s)
		            {
		                *p_MF++ *= (*p_row++);
		            }
		            
		            // run the reverse, MappedField (reused) //
		            dct_bw = fftwf_plan_r2r_2d(M-2, N-2,data_dct, MappedField,FFTW_RODFT00, FFTW_RODFT00,FFTW_ESTIMATE | FFTW_DESTROY_INPUT);
		            //dct_bw = fftwf_plan_r2r_2d(M-2, N-2,data_dct, MappedField,FFTW_REDFT01, FFTW_REDFT01,FFTW_ESTIMATE | FFTW_DESTROY_INPUT);
		            fftwf_execute(dct_bw);
		            

		            //normalization because of fftw 
		            p_MF = MappedField;
		            for (int s = 0; s < Total; ++s)
		            {
		                *p_MF++ *= normalization;
		            }

		            //shift such that the mean of result and original are the same
		            float mean_result = 0;
		            p_MF = MappedField;
		            for (int s = 0; s < Total; ++s)
		            {
		                mean_result += (*p_MF++);
		            }
		            mean_result /= Total;

		            //the 0.5 is for the round into unsigned char
		            DataType mean_offset = mean_orig.val[i] - mean_result + 0.5;

		            //put it into result_one_channel
		            p_row = MappedField + (Pad_SizeR-1)*(N-2)+Pad_SizeC-1;

		            for (int k = 0; k < channels.rows; ++k)
		            {
		                p_result = result[j].ptr<Vec3b>(k);
		                for (int q = 0; q < channels.cols; ++q)
		                {
		                    tmp = (*p_row++) + mean_offset;
		                    if(tmp<=0) 
		                    {
		                        p_result[q][i] = 0;
		                    }else if (tmp>=255)
		                    {
		                        p_result[q][i] = 255;
		                    }else
		                    {
		                        p_result[q][i] = (unsigned char)(tmp);
		                    }
		                }
		                p_row += (2*Pad_SizeC-2);
		            }
		        }  
		    }
		}
	}
	
	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) 
	{
		
	}
}