package mosaic.core.particleLinking;	

public abstract class JohnsonTrotter 
{
	int[] p;
	int[] pi;
	int[] dir;
		
	public void permInit(int N)
	{
		p   = new int[N];     // permutation
		pi  = new int[N];     // inverse permutation
		dir = new int[N];     // direction = +1 or -1
		for (int i = 0; i < N; i++) 
		{
			dir[i] = -1;
			p[i]  = i;
			pi[i] = i;
		}
	}

	public void perm(int n)
	{ 
		// base case - print out permutation
		if (n >= p.length) 
		{
			for (int i = 0; i < p.length; i++)
				System.out.print(p[i]);
			return;
		}

		perm(n+1);
		for (int i = 0; i <= n-1; i++) 
		{
			// swap 
			System.out.printf("   (%d %d)\n", pi[n], pi[n] + dir[n]);
			int z = p[pi[n] + dir[n]];
			p[pi[n]] = z;
			p[pi[n] + dir[n]] = n;
			pi[z] = pi[n];
			pi[n] = pi[n] + dir[n];  

			perm(n+1); 
		}
		dir[n] = -dir[n];
	}
		
		
	int [] getP()
	{
		return p;
	}
	
	abstract void Permutation();
}
	
	