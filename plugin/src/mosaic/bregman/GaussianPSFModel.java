package mosaic.bregman;

import mosaic.psf2d.PsfBessel;

public class GaussianPSFModel {

		private double NA, r, n;
		private double kex, kem;
		private double lex, lem;
		double c1, c2;
		double airy_unit;
		//PsfBessel bessel;
		
		public  GaussianPSFModel(double vlem, double vlex, double vna, double vr, double vn){
			this.lem=vlem/1000;
			this.lex=vlex/1000;
			this.NA=vna;
			this.airy_unit= 1.22*lex/NA;

			this.r=vr*airy_unit;
			this.n=vn;
			
			//kex and kem (same l for both)
			this.kex=2*Math.PI/lex;
			this.kem=2*Math.PI/lem;
			
			this.c1= kex*r*NA;
			this.c2= kem*r*NA;
			//bessel = new PsfBessel();
		}
	
	
		double lateral_WFFM(){
			double res;
			res= Math.sqrt(2)/(kem*NA);
			return res;
		}
		
		double axial_WFFM(){
			double res;
			res= 2*Math.sqrt(6)*n/(kem*NA*NA);
			return res;
		}
		
		
		double lateral_LSCM(){
			double res;
			double j0=PsfBessel.j0(c2);
			double j1=PsfBessel.j1(c2);
			double num=4*c2*PsfBessel.j0(c2)*PsfBessel.j1(c2) - 8*Math.pow(PsfBessel.j1(c2),2);
			double den=Math.pow(r,2)*(Math.pow(PsfBessel.j0(c2),2)+ Math.pow(PsfBessel.j1(c2),2) -1);
			
			
			res= Math.sqrt(2) / Math.sqrt((c1*c1/(r*r)) + num/den);
			return res;
		}
		
		
		double axial_LSCM(){
			double res;
			
			double num=48*c2*c2*(Math.pow(PsfBessel.j0(c2),2)+ Math.pow(PsfBessel.j1(c2),2)) - 192*Math.pow(PsfBessel.j1(c2),2);
			double den=Math.pow(n,2)*Math.pow(kem,2)*Math.pow(r,4)*(Math.pow(PsfBessel.j0(c2),2)+ Math.pow(PsfBessel.j1(c2),2) -1);
			
			
			res= 2*Math.sqrt(6) / Math.sqrt((c1*c1*NA*NA/(r*r*n*n)) - num/den);
			return res;
		}		
		
		
		
//		int gaussian_sigma(double* sz, double* sr, double lex, double lem,
//                double NA, double n, double r, int widefield, int paraxial)
//{
// if ((NA <= 0.0) || (n <= 0.0) || (lem <= 0.0) || ((NA/n) >= 1.0))
//     return -1;
//
// if (widefield) {
//     if (paraxial) {
//         *sr = sqrt(2.) * lem / (M_2PI * NA);
//         *sz = sqrt(6.) * lem / (M_2PI * NA*NA) * n * 2.;
//     } else {
//         sigma_widefield(sz, sr, n*M_2PI/lem, cos(asin(NA/n)));
//     }
// } else {
//     if ((r <= 0.0) || (lex <= 0.0))
//         return -1;
//     if (paraxial) {
//         double kem = M_2PI / lem;
//         double c1 = M_2PI / lex * r * NA;
//         double c2 = M_2PI / lem * r * NA;
//         double J0, J1, J[3];
//         bessel_lookup(c2, J);
//         J0 = J[0];
//         J1 = J[1];
//         *sr = sqrt(2./(c1*c1/(r*r) +
//             (4.*c2*J0*J1 - 8.*J1*J1) / (r*r*(J0*J0 + J1*J1 - 1.))));
//         *sz = 2.*sqrt(6./((c1*c1*NA*NA)/(r*r*n*n) -
//             (48.*c2*c2*(J0*J0 + J1*J1) - 192.*J1*J1) /
//             (n*n*kem*kem*r*r*r*r*(J0*J0 + J1*J1 - 1.))));
//     } else {
//         double e, sz_em, sr_em, sz_ex, sr_ex;
//         double cosa = cos(asin(NA/n));
//         sigma_widefield(&sz_ex, &sr_ex, n*M_2PI/lex, cosa);
//         sigma_widefield(&sz_em, &sr_em, n*M_2PI/lem, cosa);
//         e = sr_em*sr_em;
//         e = 2.0 * e*e * (exp(r*r/(2.0*e)) - 1.0);
//         *sr = sqrt((sr_ex*sr_ex * e) / (e + r*r * sr_ex*sr_ex));
//         *sz = sz_ex*sz_em / sqrt(sz_ex*sz_ex + sz_em*sz_em);
//     }
// }
// return 0;
//}
		
		
		
	
}
