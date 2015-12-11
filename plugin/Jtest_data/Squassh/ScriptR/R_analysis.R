#R_analysis v1.5 

###Mandatory parameters###################################################################### 

##file names 
file_1="/tmp/test/stitch__ObjectsData_c1.csv"  #(objA channel)
file_2="/tmp/test/stitch__ObjectsData_c2.csv"  #(objB channel)
file_3="/tmp/test/stitch_ImagesData.csv"  #(images mean results)

#Data Properties 
NR=1
NCR=c(1) #Number of images per group (should have as many values as number of groups)

#display parameters 
objA="channel 1 name" 	#ch1 name
objB="channel 2 name" 	#ch2 name
ConditionsNames=c("Condition 1 name") #group names (should have as many names as number of groups)

###########################################################################################
###Optional parameters#####################################################################
###########################################################################################
##Thresholds to remove some objects or images from the analysis 
MinIntCh1=0  #minimum object intensity ch1
MinIntCh2=0  #minimum object intensity ch2
MaxSize=100000 # maximum object size
MinSize=0    # minimum object size
MinObjects=5       # minimum number of objects in an image

#swap=c(3,2,1,4) #change display ordering of the groups 
# also posisble to remove some conditions from the analysis e.g swap= c(1,3,4) will only perform analysis on conditions 1, 3 and 4
#swap=c(3,2,1,4) on this examples displays R5a then R4a, R11b and R7a (it removes R9 from the analysis) 
swap=seq(1,NR,1) # uncomment for no reordering


MaxColocDisp=75  # maximum Y display axis
Overlap=0.5 # overlap threshold for colocalization based on number of objects

###directory names
colocalization_directory="colocalization_analysis"

##Use only a subset of the images in each group :
NC2= NCR #set NC2 =c(4,5, ..) to use only first 4 images in first group and 5 images in second group ...
##(values should be less or equalto those set in NCR)


#size ranges parameters (should be sequences of 4 values each)
#to test sequence of threholds for size and intensity effect
minrange=seq(2,20,6) #minsize range : 2 8 14 20
minintrange=seq(0.05,0.5,0.15) #min intensity range 0.05 0.20 0.35 0.50
maxrange=seq(100,2500,800) #maxsize range 100 900 1700 2500


###########verif parameters
if(NR != length(NCR)){ 
	stop(paste0("'NCR' (line 14) should contain ", NR, " values (as defined in parameter NR)" ), call.= FALSE)
	 }
	 
if(NR != length(ConditionsNames)){ 
	stop(paste0("'ConditionsNames' (line 19) should contain ", NR, " group names (as defined in parameter NR)" ), call.= FALSE)
	 }	 

ming=min(NCR)

###########################################
###Script Code (should not be changed)#####
###########################################
MinV=MinObjects
#NR2=length(swap)
#secondary_results_directory="secondary_results_H4B4m"

namesConditions= ConditionsNames
dir1=colocalization_directory
#dir2=secondary_results_directory

dir.create(dir1,showWarnings = FALSE)
#dir.create(dir2,showWarnings = FALSE)
dir1=paste0(dir1,.Platform$file.sep)
dircsv=paste0(dir1,"data",.Platform$file.sep)
dir.create(dircsv,showWarnings = FALSE)

#dir2=paste0(dir2,.Platform$file.sep)
dir2=dir1
Coloc1name=paste0(" ( ", objA," + ", objB," ) ", " / ", objA)
Coloc2name=paste0(" ( ", objA," + ", objB," ) ", " / ", objB)

##########
##load first channel data
data=read.csv(file_1, sep = ";", comment.char="%")

dtl=NULL
#Split data
#first set of data
    dtl=c(dtl,list(data[data$Image_ID<NCR[1],]))
sum=NCR[1]
#other sets
if(NR>1){
    for( i in 2:NR ){
    dtl=c(dtl,list(data[data$Image_ID>(sum-1) & data$Image_ID<(sum+NCR[i]) ,]))
    sum=sum+NCR[i]
    }
    }


##########
##load rab data
if(file_2 != "null")
{
	datar=read.csv(file_2, sep = ";", comment.char="%")
	dtlr=NULL
	#Split data
	#first set of data
	    dtlr=c(dtlr,list(datar[datar$Image_ID<NCR[1],]))
	sum=NCR[1]
	#other sets
	if(NR>1){
	    for( i in 2:NR ){
	    dtlr=c(dtlr,list(datar[datar$Image_ID>(sum-1) & datar$Image_ID<(sum+NCR[i]) ,]))
	    sum=sum+NCR[i]
	    }
	    }
	    
    }

##########
##load image average data
data_all=read.csv(file_3, sep = ";", comment.char="%")
nr_all=nrow(data_all)
param_line=as.character(data_all[nr_all,1])
param_line=sub("Min intensity ch1", "\nMin intensity ch1", param_line)
param_line=sub("Cell mask ch2", "\nCell mask ch2", param_line)
param_line=sub("Parameters", "Plugin parameters", param_line)

script_param=sprintf("R script analysis parameters: MinIntCh1 %.3f MinIntCh2 %.3f MaxSize %d MinSize %d MinObjects %d", MinIntCh1,MinIntCh2,MaxSize,MinSize,MinObjects)
data_all=data_all[seq(1,nr_all,1),]


if(sum(NCR) > (nr_all)){ 
	stop(paste0("Not enough images in the data, correct NCR line 13" ), call.= FALSE)
	 }	 
	 
##test 3D or 2D data :	 
test3D = FALSE	 
if(mean(dtl[[1]]$Coord_Z != 0)){
	test3D = TRUE
}

##########
marker=objA
namesRAB=namesConditions
namesRABc=namesRAB
namesRABc[1]=paste0(",", namesRABc[1])
#namesRABc=c(",",namesConditions)
#make column name
ChannelNameA=paste0(objA, "_")
ChannelNameB=paste0(objB, "_")

channel_number=1 #channel number of objA
o_channel_number=0
	   if (channel_number==2) {
		        o_channel_number=1
		  	}else{
		 	 o_channel_number=2
				}

OverlapName=paste0("Overlap_with_ch")

##########list[x,y] construct
list <- structure(NA,class="result")
"[<-.result" <- function(x,...,value) {
   args <- as.list(match.call())
   args <- args[-c(1:2,length(args))]
   length(value) <- length(args)
   for(i in seq(along=args)) {
     a <- args[[i]]
     if(!missing(a)) eval.parent(substitute(a <- v,list(a=a,v=value[[i]])))
   }
   x
}

#################
#Coloc (number of objects based) function
#################
coloc = function(d1, d2, Int, Overlap, ColocInt, MaxSize, MinSize, NR, OverlapName){
      #create variables...
      rd0=matrix(nrow=max(NC2),ncol=NR)
      m=1
      s=1
      sum=0
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1) #or NCR[rab] to use all
          for( i in 1:NC2[rab] ){
    	   n1=nrow(d1[[rab]][d1[[rab]]$Image_ID==(sum+(i-1)) & d1[[rab]]$Intensity>Int & d1[[rab]]$Size>MinSize & d1[[rab]]$Size<MaxSize,])
    	   n2=nrow(d2[[rab]][d2[[rab]]$Image_ID==(sum+(i-1)) & d2[[rab]]$Intensity>ColocInt & d2[[rab]]$Size>MinSize & d2[[rab]]$Size<MaxSize,])
    	   c1=nrow(d1[[rab]][d1[[rab]]$Image_ID==(sum+(i-1)) & d1[[rab]]$Intensity>Int & d1[[rab]]$Size>MinSize & d1[[rab]]$Size<MaxSize & d1[[rab]][,OverlapName]>Overlap & d1[[rab]]$Coloc_object_intensity>ColocInt & d1[[rab]]$Coloc_object_size<MaxSize & d1[[rab]]$Coloc_object_size>MinSize,])
	   if (n1<MinV || n2<MinV) {
		        rd[i,1] = NA
		  	}	 
			 else{
		 	 rd[i,1]=c1/n1
			 }
			 rd0[i,rab]=rd[i,1]
   			   }
			   sum=sum+NCR[rab]
			   m[rab]=mean(rd[,1],na.rm =TRUE)
			     s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			}


    result = data.frame(mean=m,
                     sem=s)
    return(list(result, rd0))
    

}


#################
#Coloc (number of objects based) function, with possibly lower number of cells taken into account (NA)
#################
colocN = function(dtl,Int, Overlap, ColocInt, MaxSize, MinSize, NR, NCA, OverlapName){
      #create variables...
      rd=matrix(nrow=NCA,ncol=NR)
      m=1
      s=1
      sum=0
      for(rab in 1:NR){
      rd=matrix(nrow=NCA,ncol=1)
          for( i in 1:NCA ){
    	   n1=nrow(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1)) & dtl[[rab]]$Intensity>Int & dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize,])
    	   c1=nrow(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1)) & dtl[[rab]]$Intensity>Int & dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize & dtl[[rab]][,OverlapName]>Overlap & dtl[[rab]]$Coloc_object_intensity>ColocInt & dtl[[rab]]$Coloc_object_size<MaxSize,])
	   if (n1==0) {
		        rd[i,1]=0
		  	}	 
			 else{
		 	 rd[i,1]=c1/n1
				}
   			   }
			  sum=sum+NCR[rab]
			  m[rab]=mean(rd[,1],na.rm =TRUE)
			  s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))

			 }

    result = data.frame(mean=m,
                     sem=s)
    result

}


#################
#Coloc (intensity based) function
#################
colocib = function(d1, d2, MaxSize, MinSize, NR, wr,OverlapName, order ){
      #create variables...
      rd0=matrix(nrow=max(NC2),ncol=NR)
      m=1
      s=1
	coInt=0
	tInt=0
	sum=0
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
	j=1
          for( i in 1:NC2[rab] ){
		dtemp=d1[[rab]][d1[[rab]]$Image_ID==(sum+(i-1)) & d1[[rab]]$Size>MinSize & d1[[rab]]$Size<MaxSize,] 
    	   	n1=nrow(dtemp)
    	   	nr=nrow(d2[[rab]][d2[[rab]]$Image_ID==(sum+(i-1)) & d2[[rab]]$Size>MinSize & d2[[rab]]$Size<MaxSize,]) 
		
	coInt=0
	tInt=0
	if(n1!=0){
		for(ves in 1:n1){
	  		coInt=coInt+dtemp[ves,OverlapName] * dtemp[ves,'Size'] * dtemp[ves,'Intensity']
			tInt=tInt+ dtemp[ves,'Size'] * dtemp[ves,'Intensity']
				}
			res=coInt/tInt
   			   }
			   else {
			   res=0
			   }
			 if(nr>MinV && n1>MinV){
			   rd[j,1]=res
			  rd0[j,rab]=res
			  j=j+1
			  }			   
			 }
			 sum=sum+NCR[rab]
			    m[rab]=mean(rd[1:(j-1)],na.rm =TRUE)
			     s[rab]=sd(rd[1:(j-1)],na.rm =TRUE)/(sqrt(length(rd[1:(j-1)])))
}

		if(wr==1){
		write.table(rd0,file=paste0(dir1,"Colocalization_images_data",order,".csv"), quote= FALSE, row.names=TRUE,col.names=namesRAB, sep=",")
		}

    result = data.frame(mean=m,
                     sem=s)
    result

}


#################
#Coloc (intensity based) function
#################
colocib_effect = function(d1, d2,  MaxSize, MinSize, NR,OverlapName, order, MinInt, MinColocInt ){
      #create variables...
      rd0=matrix(nrow=max(NC2),ncol=NR)
      m=1
      s=1
	coInt=0
	tInt=0
	sum=0
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
	j=1
          for( i in 1:NC2[rab] ){
		dtemp=d1[[rab]][d1[[rab]]$Image_ID==(sum+(i-1)) & d1[[rab]]$Size>MinSize & d1[[rab]]$Size<MaxSize & d1[[rab]]$Intensity>MinInt,] 
    	   	n1=nrow(dtemp)
    	   	nr=nrow(d2[[rab]][d2[[rab]]$Image_ID==(sum+(i-1)) & d2[[rab]]$Size>MinSize & d2[[rab]]$Size<MaxSize & d2[[rab]]$Intensity > MinColocInt,]) 
		
	coInt=0
	tInt=0
	if(n1!=0){
		ov=0
		for(ves in 1:n1){
			if(dtemp[ves,'Coloc_object_size'] > MaxSize || dtemp[ves,'Coloc_object_size'] < MinSize || dtemp[ves,'Coloc_object_intensity'] < MinColocInt ){
				ov=0
				}else{ ov = dtemp[ves,OverlapName]} 
				
	  		coInt=coInt+ ov * dtemp[ves,'Size'] * dtemp[ves,'Intensity']
			tInt=tInt+ dtemp[ves,'Size'] * dtemp[ves,'Intensity']
				}
			res=coInt/tInt
   			   }
			   else {
			   res=0
			   }
			 if(nr>MinV && n1>MinV){
			   rd[j,1]=res
			  rd0[j,rab]=res
			  j=j+1
			  }			   
			 }
			 sum=sum+NCR[rab]
			    m[rab]=mean(rd[1:(j-1)],na.rm =TRUE)
			     s[rab]=sd(rd[1:(j-1)],na.rm =TRUE)/(sqrt(length(rd[1:(j-1)])))
}



    result = data.frame(mean=m,
                     sem=s)
    return(list(result, rd0))

}



colocib_size_only = function(d1, d2,  MaxSize, MinSize, NR,OverlapName, order, MinInt, MinColocInt ){
      #create variables...
      rd0=matrix(nrow=max(NC2),ncol=NR)
      m=1
      s=1
	coInt=0
	tInt=0
	sum=0
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
	j=1
          for( i in 1:NC2[rab] ){
		dtemp=d1[[rab]][d1[[rab]]$Image_ID==(sum+(i-1)) & d1[[rab]]$Size>MinSize & d1[[rab]]$Size<MaxSize & d1[[rab]]$Intensity>MinInt,] 
    	   	n1=nrow(dtemp)
    	   	nr=nrow(d2[[rab]][d2[[rab]]$Image_ID==(sum+(i-1)) & d2[[rab]]$Size>MinSize & d2[[rab]]$Size<MaxSize & d2[[rab]]$Intensity > MinColocInt,]) 
		
	coInt=0
	tInt=0
	if(n1!=0){
		ov=0
		for(ves in 1:n1){
				if(dtemp[ves,'Coloc_object_size'] > MaxSize || dtemp[ves,'Coloc_object_size'] < MinSize || dtemp[ves,'Coloc_object_intensity'] < MinColocInt ){
				ov=0
				}else{ ov = dtemp[ves,OverlapName]} #objects colocalizing with non valid objects are considered non colocalizing
			
			
	  		coInt=coInt + ov * dtemp[ves,'Size']
			tInt=tInt+ dtemp[ves,'Size']
				}
			res=coInt/tInt
   			   }
			   else {
			   res=0
			   }
			 if(nr>MinV && n1>MinV){
			   rd[j,1]=res
			  rd0[j,rab]=res
			  j=j+1
			  }			   
			 }
			 sum=sum+NCR[rab]
			    m[rab]=mean(rd[1:(j-1)],na.rm =TRUE)
			     s[rab]=sd(rd[1:(j-1)],na.rm =TRUE)/(sqrt(length(rd[1:(j-1)])))
}

    result = data.frame(mean=m,
                     sem=s)
                     
    return(list(result, rd0))

}


#################
#Coloc (intensity based) function
#################
colocibN = function(dtl, MaxSize, MinSize, NR, NCA, OverlapName){
      #create variables...
      rd0=matrix(nrow=max(NC2),ncol=NR)
      m=1
      s=1
	coInt=0
	tInt=0
	sum=0
      for(rab in 1:NR){
      rd=matrix(nrow=NCA,ncol=1)
          for( i in 1:NCA ){
		dtemp=dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1)) & dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize,] 
    	   	n1=nrow(dtemp)
	coInt=0
	tInt=0
	if(n1!=0){
		for(ves in 1:n1){
	  		coInt=coInt+dtemp[ves,OverlapName] * dtemp[ves,'Size'] * dtemp[ves,'Intensity']
			tInt=tInt+ dtemp[ves,'Size'] * dtemp[ves,'Intensity']
				}

			rd[i,1]=coInt/tInt
   			   }

			   else {
			   rd[i,1]=0
			   }
			  rd0[i,rab]=rd[i,1]			   
			 }
			 sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			     s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
}

    result = data.frame(mean=m,
                     sem=s)
    result

}


#################
#Pearson numbers
#################
pearson = function(data, NR){
      #create variables...
      #rd=matrix(nrow=NC,ncol=NR)
      m=1
      s=1
	  sum=0
	  rd0=matrix(nrow=max(NC2),ncol=NR)
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
    	
		 	 rd[i,1]=data_all[data_all$Image.ID==(sum +i) -1 , 'Pearson.correlation']
			 rd0[i,rab]=rd[i,1]
   			   }
			    sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			     s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			     
			 }


    result = data.frame(mean=m,
                     sem=s)
    return(list(result, rd0))

}


#################
#Pearson in mask
#################
pearson_mask = function(data, NR){
      #create variables...
      #rd=matrix(nrow=NC,ncol=NR)
      m=1
      s=1
		sum=0
		rd0=matrix(nrow=max(NC2),ncol=NR)
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
		 	 rd[i,1]=data_all[data_all$Image.ID==(sum +i) -1 , 'Pearson.correlation.inside.cell.masks']
   			   rd0[i,rab]=rd[i,1]
   			   }
			    sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			     s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			     
			 }


    result = data.frame(mean=m,
                     sem=s)
    return(list(result, rd0))
}


#################
#Vesicle numbers
#################
mean_ves_number = function(dtl, MaxSize, MinSize, NR){
      #create variables...
      #rd=matrix(nrow=NC,ncol=NR)
      m=1
      s=1
      sum=0
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
    	   n=nrow(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1))  & dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize ,])
    	
		 	 rd[i,1]=n
	
   			   }
			    sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			     s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			 }


    result = data.frame(mean=m,
                     sem=s)
    result

}

#################
#Vesicle sizes
#################
mean_ves_size = function(dtl, MaxSize, MinSize, NR){
      #create variables...
      #rd=matrix(nrow=NC,ncol=NR)
      m=1
      s=1
      sum=0
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
    	  colves=(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1))& dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize ,])
    	
		 	 rd[i,1]=mean(colves$Size,na.rm =TRUE)
	
   			   }
   			   #colves=(dtl[[rab]][dtl[[rab]]$Image_ID>(sum-1) &dtl[[rab]]$Image_ID<sum+NCR[rab]& dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize ,])

   			   
			    sum=sum+NCR[rab]
			    #m[rab]=mean(colves$Size)
				#s[rab]=sd(colves$Size)/sqrt(length(colves$Size))
				
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			    s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			 }



    result = data.frame(mean=m,
                     sem=s)
    result

}


#################
#Vesicle sizes
#################
mean_ves_length = function(dtl, MaxSize, MinSize, NR){
      #create variables...
      #rd=matrix(nrow=NC,ncol=NR)
      m=1
      s=1
      sum=0
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
    	   colves=(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1))& dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize ,])
    	
		 	 rd[i,1]=mean(colves$Length,na.rm =TRUE)
	
   			   }
			    sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			     s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			 }



    result = data.frame(mean=m,
                     sem=s)
    result

}


#################
#Vesicle total size
#################
mean_total_size = function(dtl, MaxSize, MinSize, NR){
      #create variables...
      #rd=matrix(nrow=NC,ncol=NR)
      m=1
      s=1
      sum=0
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
    	   colves=(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1))& dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize ,])
    	
		 	 rd[i,1]=sum(colves$Size)
	
   			   }
   			   #colves=(dtl[[rab]][dtl[[rab]]$Image_ID>(sum-1) &dtl[[rab]]$Image_ID<sum+NCR[rab]& dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize ,])
				#n= nrow((dtl[[rab]][dtl[[rab]]$Image_ID>(sum-1) &dtl[[rab]]$Image_ID<sum+NCR[rab]& dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize ,]))
			    sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			    s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			 }



    result = data.frame(mean=m,
                     sem=s)
    result

}

####################
#Vesicle total size#
####################
mean_total_size_ratio = function(dtl,dtl2, MaxSize, MinSize, NR){
      #create variables...
      #rd=matrix(nrow=NC,ncol=NR)
      m=1
      s=1
      sum=0
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
    	   colves=(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1))& dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize ,])
		 	 colves2=(dtl2[[rab]][dtl2[[rab]]$Image_ID==(sum+(i-1))& dtl2[[rab]]$Size>MinSize & dtl2[[rab]]$Size<MaxSize ,])
		 	 rd[i,1]=sum(colves$Size) / sum(colves2$Size)	
   			   }
			    sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			    s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			 }



    result = data.frame(mean=m,
                     sem=s)
    result

}

#################
#Vesicle sphericity
#################
mean_ves_sphericity = function(dtl, NR){
      #create variables...
      #rd=matrix(nrow=NC,ncol=NR)
      m=1
      s=1
      sum=0
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
    	   colves=(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1)),])
    	
    	#remove zeros in Perimeter column (due to probleme in code, corrected now)
    	k=0
		for(t in colves$Perimeter==0){
			k=k+1
			if(t){
			colves$Perimeter[k] = colves$Perimeter[k] + 1
			}

			}
			
    	
		 	 rd[i,1]=mean((pi**(1/3))*((6*(colves$Size))**(2/3))/(colves$Perimeter),na.rm =TRUE)
   			   
   			   }
   			   
			    sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			     s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			 }



    result = data.frame(mean=m,
                     sem=s)
    result

}


#################
#Rab Vesicle intensities
#################
mean_ves_int = function(dtl, MaxSize, MinSize, NR){
      #create variables...
      m=1
      s=1
      sum=0
#number of vesicles
      for(rab in 1:NR){
      rd=matrix(nrow=NC2[rab],ncol=1)
          for( i in 1:NC2[rab] ){
    	   colves=(dtl[[rab]][dtl[[rab]]$Image_ID==(sum+(i-1))  & dtl[[rab]]$Size>MinSize & dtl[[rab]]$Size<MaxSize,])
    	
		 	 rd[i,1]=mean(colves$Intensity,na.rm =TRUE)

   			   }

			    sum=sum+NCR[rab]
			    m[rab]=mean(rd[,1],na.rm =TRUE)
			     s[rab]=sd(rd[,1],na.rm =TRUE)/(sqrt(length(rd[,1])))
			 }


    result = data.frame(mean=m,
                     sem=s)
    result

}



#################
#error bar function
#################
error.bar <- function(x, y, upper, lower=upper, length=0.1,...){
if(length(x) != length(y) | length(y) !=length(lower) | length(lower) != length(upper))
stop("vectors must be same length")
if( !is.na(max(upper)) ){
	suppressWarnings(arrows(x,y+upper, x, y-lower, angle=90, code=3, length=length, ...))
	}
}



#################
#Colocalization plot function
#################
### bar plot
plotres=function(res, title, ylim, ylabel){
    barx <- barplot(100*res$mean, names.arg=namesRAB[swap],ylim=c(0,ylim), col="blue", axis.lty=1, xlab=title, ylab=ylabel)
    error.bar(barx,100*res$mean, 100*res$sem)
    #box("figure", lty="dotted", col = "blue")
}

plotresnumbers=function(res, xtitle, ytitle, ymax){
    barx <- barplot(res$mean, names.arg=namesRAB[swap], col="blue",ylim=c(0,ymax), axis.lty=1, xlab=xtitle, ylab=ytitle)
    error.bar(barx,res$mean, res$sem)
}

plotresnumbers_min=function(res, xtitle, ytitle, ymax, ymin){
    barx <- barplot(res$mean, names.arg=namesRAB[swap], col="blue",ylim=c(ymin,ymax), axis.lty=1, xlab=xtitle, ylab=ytitle)
    error.bar(barx,res$mean, res$sem)
   #box("figure", lty="dotted", col = "blue")
}

#################
#Colocalization effect plot function
#################
### bar plot
plotresf=function(means,sems, teffect, ymax, ylabel){
barx <- barplot(100*means, beside=TRUE, ylim=c(0,ymax), names.arg=namesRAB[swap], axis.lty=1, xlab=teffect, ylab=ylabel)
 error.bar(barx,100*means,100*sems)
}

### bar plot
plotres_err=function(means, teffect, ymax){
barx <- barplot(100*means, beside=TRUE, ylim=c(0,ymax), names.arg=namesRAB[swap], axis.lty=1, xlab=teffect, ylab="Error (SEM)")
}


####################################################################################
##################     Statistical analysis       ##################################
pvstr = function(pval){
	
	if(is.nan(pval)) {pstring=sprintf("Nan")}
	else if(pval < 0.0001){pstring=sprintf("< 1e-4, ****" )}
	else if(pval < 0.001){pstring=sprintf("%.2e, ***",pval )}
	else if(pval < 0.01){pstring=sprintf("%.2e, **",pval )}
	else if(pval < 0.05){pstring=sprintf("%.2e, *",pval )}
	else{pstring= sprintf("%.2e, ns",pval )}
	
	return(pstring)
	
}


stat_aov_tukey= function(data){
	var = as.vector(data)
	cond = rep (namesRAB[swap],rep(max(NC2),length(swap)))
	data2= data.frame(var, cond) 
	aovr = aov(var ~ cond , data2) #1 way anova 
	tuk = TukeyHSD(aovr)# tukey test
	
	pval = summary(aovr)[[1]][1,"Pr(>F)"]
	pstring=pvstr(pval)
	
	return(list(pstring, pval, aovr, tuk))
} 


tukeyplot100 = function (x, minp, maxp, xlabel)#, ylabel) 
{
    for (i in seq_along(x)) {
        xi <- 100*x[[i]][, -4, drop = FALSE]
        yvals <- nrow(xi):1
        dev.hold()
        on.exit(dev.flush())
        plot(c(xi[, "lwr"], xi[, "upr"]), rep.int(yvals, 2), 
            type = "n", axes = FALSE, xlab = xlabel, ylab = "", main = "", xlim=c(100*minp,100*maxp))
        axis(1)
        ynames=dimnames(xi)[[1L]]
        ypvalues=sapply(x$cond[,4], pvstr)
        ylabs=paste0(ynames,", ", ypvalues)
        axis(2, at = nrow(xi):1, labels = ylabs, 
            srt = 0)
        abline(h = yvals, lty = 1, lwd = 0.5, col = "lightgray")
        abline(v = 0, lty = 2, lwd = 0.5)
        segments(xi[, "lwr"], yvals, xi[, "upr"], yvals)
        segments(as.vector(xi), rep.int(yvals - 0.1, 3), as.vector(xi), 
            rep.int(yvals + 0.1, 3))
       # title(main = paste(format(100 * attr(x, "conf.level"), 
       #     digits = 2), "% family-wise confidence level\n", 
       #     sep = ""), xlab = paste("Differences in mean levels of", 
       #     names(x)[i]))
        box()
    }
}


tukeyplot = function (x, minp, maxp, xlabel)#, ylabel) 
{
    for (i in seq_along(x)) {
        xi <- x[[i]][, -4, drop = FALSE]
        yvals <- nrow(xi):1
        dev.hold()
        on.exit(dev.flush())
        plot(c(xi[, "lwr"], xi[, "upr"]), rep.int(yvals, 2), 
            type = "n", axes = FALSE, xlab = xlabel, ylab = "", main = "", xlim=c(minp,maxp))
        axis(1)
        ynames=dimnames(xi)[[1L]]
        ypvalues=sapply(x$cond[,4], pvstr)
        ylabs=paste0(ynames,", ", ypvalues)
        axis(2, at = nrow(xi):1, labels = ylabs, 
            srt = 0)
        abline(h = yvals, lty = 1, lwd = 0.5, col = "lightgray")
        abline(v = 0, lty = 2, lwd = 0.5)
        segments(xi[, "lwr"], yvals, xi[, "upr"], yvals)
        segments(as.vector(xi), rep.int(yvals - 0.1, 3), as.vector(xi), 
            rep.int(yvals + 0.1, 3))
       # title(main = paste(format(100 * attr(x, "conf.level"), 
       #     digits = 2), "% family-wise confidence level\n", 
       #     sep = ""), xlab = paste("Differences in mean levels of", 
       #     names(x)[i]))
        box()
    }
}


#####################
##calls :



#compute coloc and plot coloc
#resc=coloc(dtl, Int, Overlap, ColocInt, MaxSize, MinSize,NR,1)
#write.table(resc,file=paste0("Colocalization",marker,ChannelName, ".csv"), quote= FALSE, row.names=namesRAB,col.names=c(",mean", "sem"), sep=",")
#pdf(paste0("Colocalization",ChannelName,".pdf"))
#plotres(resc[swap,],paste(marker," in RABx",sep=''), MaxColocDisp)
#dev.off()



if(file_2 != "null")
{
####################################################################################
####################################################################################
print('Colocalization ...')

pdf(paste0(dir1,"Colocalization",".pdf"), width=8.2, height=11.3)
par(mfrow=c(4,2), mar= c(4, 4, 1, 1) + 0.1, oma =c(5,0,2.5,0.2) )


#channel A
#compute coloc and plot coloc
order= paste0("_",objA,"_in_", objB)
list[resc,res_imgs]=coloc(dtl, dtlr,MinIntCh1,Overlap,MinIntCh2,MaxSize, MinSize,NR,OverlapName)

write.table(resc,file=paste0(dircsv,"Colocalization_number",order, ".csv"), quote= FALSE, row.names=namesRAB,col.names=c(",mean", "sem"), sep=",")
write.table(res_imgs,file=paste0(dircsv,"Colocalization_images_data_number",order,".csv"), quote= FALSE, row.names=TRUE,col.names=namesRABc, sep=",")


if(NR >1 && ming>1) {list[ps,p,a,t1]=stat_aov_tukey(res_imgs[,swap]) 
	} else {
		ps = NA
		}


plotres(resc[swap,],paste0("Object number colocalization",", P value ", ps), MaxColocDisp, Coloc1name)
#dev.off()


#channel B
#compute coloc and plot coloc
order= paste0("_",objB,"_in_", objA)
list[resc,res_imgs]=coloc(dtlr, dtl,MinIntCh2,Overlap,MinIntCh1,MaxSize, MinSize,NR, OverlapName)

write.table(resc,file=paste0(dircsv,"Colocalization_number",order, ".csv"), quote= FALSE, row.names=namesRAB,col.names=c(",mean", "sem"), sep=",")
write.table(res_imgs,file=paste0(dircsv,"Colocalization_images_data_number",order,".csv"), quote= FALSE, row.names=TRUE,col.names=namesRABc, sep=",")

if(NR >1 && ming>1) {list[ps,p,a,t2]=stat_aov_tukey(res_imgs[,swap]) 
	} else {
		ps = NA
		}

plotres(resc[swap,],paste0("Object number colocalization",", P value ", ps), MaxColocDisp, Coloc2name)
#dev.off()


#flush.console()

####################################################################################
####################################################################################
order= paste0("_",objA,"_in_", objB)
list[resc,res_imgs]=colocib_size_only(dtl, dtlr, MaxSize, MinSize,NR, OverlapName, order,MinIntCh1,MinIntCh2)

write.table(resc,file=paste0(dircsv,"Colocalization_size",order, ".csv"), quote= FALSE, row.names=namesRAB,col.names=c(",mean", "sem"), sep=",")
write.table(res_imgs,file=paste0(dircsv,"Colocalization_images_data_size",order,".csv"), quote= FALSE, row.names=TRUE,col.names=namesRABc, sep=",")
if(NR >1 && ming>1) {list[ps,p,a,t3]=stat_aov_tukey(res_imgs[,swap]) 
	} else {
		ps = NA
		}

plotres(resc[swap,],paste0("Size colocalization",", P value ", ps), MaxColocDisp, Coloc1name )
#dev.off()


#channel B
#compute coloc and plot coloc
order= paste0("_",objB,"_in_", objA)
list[resc,res_imgs]=colocib_size_only(dtlr,dtl, MaxSize, MinSize,NR, OverlapName, order,MinIntCh2,MinIntCh1)

write.table(resc,file=paste0(dircsv,"Colocalization_size",order, ".csv"), quote= FALSE, row.names=namesRAB,col.names=c(",mean", "sem"), sep=",")
write.table(res_imgs,file=paste0(dircsv,"Colocalization_images_data_size",order,".csv"), quote= FALSE, row.names=TRUE,col.names=namesRABc, sep=",")
if(NR >1 && ming>1) {list[ps,p,a,t4]=stat_aov_tukey(res_imgs[,swap]) 
	} else {
		ps = NA
		}

plotres(resc[swap,],paste0("Size colocalization",", P value ", ps), MaxColocDisp, Coloc2name)
#dev.off()

#print('...done')
#flush.console()


####################################################################################
####################################################################################


#channel A
#compute coloc and plot coloc
order= paste0("_",objA,"_in_", objB)
list[resc,res_imgs]=colocib_effect(dtl, dtlr, MaxSize, MinSize,NR, OverlapName, order,MinIntCh1,MinIntCh2)

write.table(resc,file=paste0(dircsv,"Colocalization_signal",order, ".csv"), quote= FALSE, row.names=namesRAB,col.names=c(",mean", "sem"), sep=",")
write.table(res_imgs,file=paste0(dircsv,"Colocalization_images_data_signal",order,".csv"), quote= FALSE, row.names=TRUE,col.names=namesRABc, sep=",")
if(NR >1 && ming>1) {list[ps,p,a,t5]=stat_aov_tukey(res_imgs[,swap]) 
	} else {
		ps = NA
		}


plotres(resc[swap,],paste0("Signal colocalization (size and intensity)",", P value ", ps), MaxColocDisp, Coloc1name )
#dev.off()


#channel B
#compute coloc and plot coloc
order= paste0("_",objB,"_in_", objA)
list[resc,res_imgs]=colocib_effect(dtlr,dtl, MaxSize, MinSize,NR, OverlapName, order,MinIntCh2,MinIntCh1)

write.table(resc,file=paste0(dircsv,"Colocalization_signal",order, ".csv"), quote= FALSE, row.names=namesRAB,col.names=c(",mean", "sem"), sep=",")
write.table(res_imgs,file=paste0(dircsv,"Colocalization_images_data_signal",order,".csv"), quote= FALSE, row.names=TRUE,col.names=namesRABc, sep=",")
if(NR >1 && ming>1) {list[ps,p,a,t6]=stat_aov_tukey(res_imgs[,swap]) 
	} else {
		ps = NA
		}

plotres(resc[swap,],paste0("Signal colocalization (size and intensity)",", P value ", ps), MaxColocDisp, Coloc2name)


print('...done')



####################################################################################
####################################################################################
print('Pearson correlation...')


#pearson (with background reduction if activated)
list[resn,res_imgs]=pearson(data_all,NR)
if(NR >1 && ming>1) {list[ps1,p,a,t7]=stat_aov_tukey(res_imgs[,swap]) 
	} else {
		ps1 = NA
		}

list[resn2,res_imgs]=pearson_mask(data_all,NR)
if(NR >1 && ming>1) {list[ps2,p,a,t8]=stat_aov_tukey(res_imgs[,swap]) 
	} else {
		ps2 = NA
		}
minval= min(resn,resn2)
if ( !is.na(minval) && minval<0) mindisp=-1 else mindisp=0
plotresnumbers_min(resn[swap,],paste0("Pearson correlation, original image",", P value ", ps1), "Pearson correlation", MaxColocDisp/100,mindisp )


#pearson with background redcution and  cell masks if activated
plotresnumbers_min(resn2[swap,],paste0("Pearson correlation with cell masks\n and background removal",", P value ", ps2), " Pearson correlation", MaxColocDisp/100,mindisp )



mtext(paste0("Colocalization, ", ming, " samples per group"), 3, line=0, adj=0.5, cex=1.2, outer=TRUE)
mtext(as.character(param_line), 1, line=2, adj=0, cex=0.55, outer=TRUE)
mtext(as.character(script_param), 1, line=3, adj=0, cex=0.55, outer=TRUE)
mtext("Mean and sem displayed, P values from one way ANOVA, **** for p value < 0.0001, ***  0.0001 < p value < 0.001, **  0.001 < p value < 0.01, *  0.01 < p value < 0.05", 1, line=4, adj=0, cex=0.55, outer=TRUE)
mtext(format(Sys.time(), "%Y-%m-%d "), cex=0.75, line=4, side=SOUTH<-1, adj=1, outer=TRUE)


#box("outer", lty="solid", col="green")
#box("inner", lty="dotted", col="green")
dev.off()
print('...done')

#flush.console()



####################################################################################
######## print Tuckey
####################################################################################

if(NR >1 && ming>1) {

pdf(paste0(dir1,"ColocalizationCI",".pdf"), width=8.2, height=11.3)
par(mfrow=c(4,2), mar= c(4, 12, 1, 0.5) + 0.1, oma =c(5,0,2.5,0.2), las=1 )

minp=min(t1$cond[,'lwr'], t2$cond[,'lwr'], t3$cond[,'lwr'],t4$cond[,'lwr'],t5$cond[,'lwr'],t6$cond[,'lwr'],t7$cond[,'lwr'],t8$cond[,'lwr'])

maxp=max(t1$cond[,'upr'], t2$cond[,'upr'], t3$cond[,'upr'],t4$cond[,'upr'],t5$cond[,'upr'],t6$cond[,'upr'],t7$cond[,'upr'],t8$cond[,'upr'])

tukeyplot100(t1, minp, maxp, "Object number colocalization")
tukeyplot100(t2, minp, maxp, "Object number colocalization")
tukeyplot100(t3, minp, maxp,"Size colocalization")
tukeyplot100(t4, minp, maxp,"Size colocalization")
tukeyplot100(t5, minp, maxp,"Signal colocalization")
tukeyplot100(t6, minp, maxp,"Signal colocalization")
tukeyplot(t7, minp, maxp,"Pearson")
tukeyplot(t8, minp, maxp,"Pearson with mask and background removal")

mtext(paste0("Tukey test, 95% family-wise confidence levels and P values, ", ming, " samples per group"), 3, line=0, adj=0.5, cex=1.2, outer=TRUE)
mtext(as.character(param_line), 1, line=2, adj=0, cex=0.55, outer=TRUE)
mtext(as.character(script_param), 1, line=3, adj=0, cex=0.55, outer=TRUE)
mtext("P values and confidence intervals from post-hoc Tukey test, **** for p value < 0.0001, ***  0.0001 < p value < 0.001, **  0.001 < p value < 0.01, *  0.01 < p value < 0.05", 1, line=4, adj=0, cex=0.55, outer=TRUE)
mtext(format(Sys.time(), "%Y-%m-%d "), cex=0.75, line=4, side=SOUTH<-1, adj=1, outer=TRUE)

dev.off()
}
#######



####################################################################################
####################################################################################
print('Mean object numbers...')

pdf(paste0(dir2,"Secondary_results_1",".pdf"), width=8.2, height=11.3)
par(mfrow=c(4,2), mar= c(4, 4, 1, 1) + 0.1, oma =c(4,0,1.5,0.2) )


#channelA
#compute ves numbers
resn=mean_ves_number(dtl, MaxSize, MinSize,NR)
resn2=mean_ves_number(dtlr, MaxSize, MinSize,NR)



#pdf(paste0(dir2,ChannelNameA,"Object_Numbers",".pdf"))
maxdisp = max(resn[1]+resn[2], resn2[1]+resn2[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resn[1], resn2[1])
plotresnumbers(resn[swap,],paste0(objA ," Object Number"), "Object #", maxdisp)
plotresnumbers(resn2[swap,],paste0(objB ," Object Number"), "Object #", maxdisp)
#dev.off()

print('...done')
flush.console()

####################################################################################
####################################################################################
print('Mean object sizes...')

#compute ves sizes A
resv=mean_ves_size(dtl, MaxSize, MinSize,NR)
resv2=mean_ves_size(dtlr, MaxSize, MinSize,NR)


if (test3D){lbl="Volume [pixels^3]"}else{lbl="Area [pixels^2]"}
maxdisp = max(resv[1]+resv[2], resv2[1]+resv2[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resv[1], resv2[1])
plotresnumbers(resv[swap,],paste0(objA ," Size"), lbl, maxdisp)
plotresnumbers(resv2[swap,],paste0(objB ," Size"), lbl, maxdisp)
#dev.off()

print('...done')
flush.console()


####################################################################################
####################################################################################
print('Mean total size...')

#compute  A
resv1=mean_total_size(dtl, MaxSize, MinSize,NR)
resv2=mean_total_size(dtlr, MaxSize, MinSize,NR)

if (test3D){lbl="Total volume [pixels^3]"}else{lbl="Total area [pixels^2]"}
maxdisp = max(resv1[1]+resv1[2], resv2[1]+resv2[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resv1[1], resv2[1])
plotresnumbers(resv1[swap,],paste0(objA ," Total size"), lbl, maxdisp)
plotresnumbers(resv2[swap,],paste0(objB ," Total size"), lbl, maxdisp)
#dev.off()


if (test3D){lbl="Total volume ratio"}else{lbl="Total area ratio"}
resv3=mean_total_size_ratio(dtl, dtlr, MaxSize, MinSize,NR)
#pdf(paste0(dir2,ChannelNameB,"Object_Lengths",".pdf"))
plotresnumbers(resv3[swap,],paste0("Total size ratio ", objA, "/", objB), lbl, 1.25*max(resv3[1]))


if (test3D){lbl="Volume and number of objects"}else{lbl="Area and number of objects"}

mtext(paste0(lbl,", ", ming, " samples per group" ), 3, line=0, adj=0.5, cex=1.2, outer=TRUE)
mtext(as.character(param_line), 1, line=2, adj=0, cex=0.55, outer=TRUE)
mtext(as.character(script_param), 1, line=3, adj=0, cex=0.55, outer=TRUE)
mtext(format(Sys.time(), "%Y-%m-%d "), cex=0.75, line=3, side=SOUTH<-1, adj=1, outer=TRUE)

dev.off()

#dev.off()
print('...done')
flush.console()

####################################################################################
####################################################################################
print('Mean object lengths...')


pdf(paste0(dir2,"Secondary_results_2",".pdf"), width=8.2, height=11.3)
par(mfrow=c(4,2), mar= c(4, 4, 1, 1) + 0.1, oma =c(4,0,1.5,0.2) )

#compute ves lengths A and B
resv1=mean_ves_length(dtl, MaxSize, MinSize,NR)
resv2=mean_ves_length(dtlr, MaxSize, MinSize,NR)

maxdisp = max(resv1[1]+resv1[2], resv2[1]+resv2[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resv1[1], resv2[1])
plotresnumbers(resv1[swap,],paste0(objA ," Length"), "Length [pixels]", maxdisp)
plotresnumbers(resv2[swap,],paste0(objB ," Length"), "Length [pixels]", maxdisp)





#dev.off()

print('...done')
#flush.console()

####################################################################################
####################################################################################
print('Mean object intensities...')

#compute ves intensities A and B
resi1=mean_ves_int(dtl, MaxSize, MinSize,NR)
resi2=mean_ves_int(dtlr, MaxSize, MinSize,NR)

maxdisp = max(resi1[1]+resi1[2], resi2[1]+resi2[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resi1[1], resi2[1])
plotresnumbers(resi1[swap,],paste0(objA ," Intensity"), "Intensity",maxdisp)
plotresnumbers(resi2[swap,],paste0(objB ," Intensity"), "Intensity",maxdisp)
#dev.off()

print('...done')
#flush.console()

####################################################################################
####################################################################################

print('Size threshold effect ch1 in ch2...')
#pdf(paste0(dir2,ChannelNameA,"Threshold_effects",".pdf"))
#layout(matrix(c(1,2,3,4), 2, 2, byrow = TRUE))

###Min Size effect on colocib
order= paste0("_",objA,"_in_", objB)
resmeans=NULL
ressems=NULL
  for(tMinSize in minrange){
  	    list[restemp,]=colocib_effect(dtl, dtlr, MaxSize, tMinSize,NR, OverlapName, order, MinIntCh1, MinIntCh2)
	resmeans=c(resmeans,restemp$mean)
	ressems=c(ressems,restemp$sem)
   			   }
means = matrix(resmeans,4,NR,byrow=TRUE)
sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0(dir2,"Min_ves_size_effect",order,".pdf"))
plotresf(means[,swap],sems[,swap],paste0("Min Size :",paste(minrange, collapse = ' '),order), MaxColocDisp, Coloc1name)
#dev.off()


###Min Size effect on colocib
##channel B

#print('Size threshold effect ch2 in ch1...')
order= paste0("_",objB,"_in_", objA)
resmeans=NULL
ressems=NULL
  for(tMinSize in minrange){
  	    list[restemp,]=colocib_effect(dtlr, dtl, MaxSize, tMinSize,NR, OverlapName, order, MinIntCh1, MinIntCh2)
	resmeans=c(resmeans,restemp$mean)
	ressems=c(ressems,restemp$sem)
   			   }
means = matrix(resmeans,4,NR,byrow=TRUE)
sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0(dir2,"Min_ves_size_effect",order,".pdf"))
plotresf(means[,swap],sems[,swap],paste0("Min Size :",paste(minrange, collapse = ' '),order), MaxColocDisp, Coloc2name)
#dev.off()

print('...done')
flush.console()

####################################################################################
####################################################################################
print('Intensity threshold ch1 in ch2...')
##Intensity effect on colocib
order= paste0("_",objA,"_in_", objB)
resmeans=NULL
ressems=NULL
  for(MinInt in minintrange){
  	    list[restemp,]=colocib_effect(dtl, dtlr, MaxSize, MinSize, NR, OverlapName, order, MinInt, MinIntCh2)
	resmeans=c(resmeans,restemp$mean)
	ressems=c(ressems,restemp$sem)
   			   }
means = matrix(resmeans,4,NR,byrow=TRUE)
sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0(dir2,"Min_intensity_effect",order,".pdf"))
plotresf(means[,swap],sems[,swap],paste0("Min Intensity :",paste(minintrange, collapse = ' '),order), MaxColocDisp, Coloc1name)
#dev.off()


#print('Intensity threshold ch2 in ch1...')
###Intensity effect on colocib
resmeans=NULL
ressems=NULL
  for(MinInt in minintrange){
  	    list[restemp,]=colocib_effect(dtlr, dtl,  MaxSize, MinSize, NR, OverlapName, order, MinInt, MinIntCh1)
	resmeans=c(resmeans,restemp$mean)
	ressems=c(ressems,restemp$sem)
   			   }
means = matrix(resmeans,4,NR,byrow=TRUE)
sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0(dir2,"Min_intensity_effect",order,".pdf"))
plotresf(means[,swap],sems[,swap],paste0("Min Intensity :",paste(minintrange, collapse = ' '),order), MaxColocDisp,Coloc2name )



mtext(paste0("Objects lengths, intensities and threshold effects, ", ming, " samples per group" ), 3, line=0, adj=0.5, cex=1.2, outer=TRUE)
mtext(as.character(param_line), 1, line=2, adj=0, cex=0.55, outer=TRUE)
mtext(as.character(script_param), 1, line=3, adj=0, cex=0.55, outer=TRUE)
mtext(format(Sys.time(), "%Y-%m-%d "), cex=0.75, line=3, side=SOUTH<-1, adj=1, outer=TRUE)

dev.off()

print('...done')
flush.console()


}
##endif file_2 ==null

if(file_2 == "null")
{

####################################################################################
####################################################################################
print('Mean object numbers...')

pdf(paste0(dir2,"Object_properties",".pdf"), width=8.2, height=11.3)
par(mfrow=c(2,2), mar= c(4, 4, 1, 1) + 0.1, oma =c(4,0,1.5,0.2) )


#channelA
#compute ves numbers
resn=mean_ves_number(dtl, MaxSize, MinSize,NR)
maxdisp = max(resn[1]+resn[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resn[1])
plotresnumbers(resn[swap,],paste0(objA ," Object Number"), "Object #", maxdisp )

#dev.off()

print('...done')
flush.console()

####################################################################################
####################################################################################
print('Mean object sizes...')

#compute ves sizes A
resv=mean_ves_size(dtl, MaxSize, MinSize,NR)

if (test3D){lbl="Volume [pixels^3]"}else{lbl="Area [pixels^2]"}

maxdisp = max(resv[1]+resv[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resv[1])
plotresnumbers(resv[swap,],paste0(objA ," Size"), lbl, maxdisp)


print('...done')
flush.console()


####################################################################################
####################################################################################
print('Mean object lengths...')



#compute ves lengths A and B
resv1=mean_ves_length(dtl, MaxSize, MinSize,NR)
maxdisp = max(resv1[1]+resv1[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resv1[1])
plotresnumbers(resv1[swap,],paste0(objA ," Length"), "Length [pixels]",maxdisp)


print('...done')
#flush.console()

####################################################################################
####################################################################################
print('Mean object intensities...')

#compute ves intensities A and B
resi1=mean_ves_int(dtl, MaxSize, MinSize,NR)

maxdisp = max(resi1[1]+resi1[2])
if(is.na(maxdisp)) maxdisp=1.1*max(resi1[1])
plotresnumbers(resi1[swap,],paste0(objA ," Intensity"), "Intensity",maxdisp)


print('...done')

lbl="Mean objects properties"

mtext(paste0(lbl,", ", ming, " samples per group" ), 3, line=0, adj=0.5, cex=1.2, outer=TRUE)
mtext(as.character(param_line), 1, line=2, adj=0, cex=0.55, outer=TRUE)
mtext(as.character(script_param), 1, line=3, adj=0, cex=0.55, outer=TRUE)
mtext(format(Sys.time(), "%Y-%m-%d "), cex=0.75, line=3, side=SOUTH<-1, adj=1, outer=TRUE)

dev.off()

	
}

####################################################################################
####################################################################################
#sphericity

#compute ves sphericity
#resi=mean_ves_sphericity(dtl,NR)
#pdf(paste0(dir2,ChannelNameA,"Vesicle_Sphericity",".pdf"))
#plotresnumbers(resi[swap,],conditions, paste0(objA ,"Vesicle Sphericity"),max(resi[1]) + 2*max(resi[2]))
#dev.off()


#compute ves sphericity
#resi=mean_ves_sphericity(dtlr,NR)
#pdf(paste0(dir2,ChannelNameB,"Vesicle_Sphericity",".pdf"))
#plotresnumbers(resi[swap,],conditions, paste0(objB ,"Vesicle Sphericity"),max(resi[1]) + 2*max(resi[2]))
#dev.off()



###number of cells effect
#resmeans=NULL
#ressems=NULL
#  for(NCA in seq(5,20,3)){
#  	    #restemp=colocN(dtl, Int, Overlap, ColocInt, MaxSize, MinSize,NR,NC, NCA)
#	    restemp=colocibN(dtl, MaxSize, MinSize, NR, NC, NCA)
#	resmeans=c(resmeans,restemp$mean)
#	ressems=c(ressems,restemp$sem)
#   			   }
#means = matrix(resmeans,6,NR,byrow=TRUE)
#sems = matrix(ressems,6,NR,byrow=TRUE)
#pdf(paste(marker,"_numberof_cells_2to20.pdf",sep=''))
#plotresf(means[,swap],sems[,swap],paste0("number of images used:",  paste(seq(5,20,3), collapse = ' ')), MaxColocDisp)
#dev.off()
#
#pdf(paste(marker,"_errorvscellsnumber_2to20.pdf",sep=''))
#plotres_err(sems[,swap], paste0("number of images used:",  paste(seq(5,20,3), collapse = ' ')), 15)
#
#dev.off()




###intensity effect
#resmeans=NULL
#ressems=NULL
#  for(tInt in seq(0.2,0.5,0.05)){
#  	    restemp=coloc(dtl, tInt, Overlap, ColocInt, MaxSize, MinSize,NR,0)
#	resmeans=c(resmeans,restemp$mean)
#	ressems=c(ressems,restemp$sem)
#   			   }
#means = matrix(resmeans,7,NR,byrow=TRUE)
#sems = matrix(ressems,7,NR,byrow=TRUE)
#pdf(paste0(marker,"_min_intensities",ChannelName,".pdf"))
#plotresf(means[,swap],sems[,swap],paste0("Min ", marker,  " intensity", paste(seq(0.2,0.5,0.05), collapse = ' ')), MaxColocDisp)
#dev.off()

###coloc intensity effect
#resmeans=NULL
#ressems=NULL
#  for(tColocInt in seq(0.1,0.4,0.1)){
#  	    restemp=coloc(dtl, Int, Overlap, tColocInt, MaxSize, MinSize,NR,0)
#	resmeans=c(resmeans,restemp$mean)
#	ressems=c(ressems,restemp$sem)
#   			   }
#means = matrix(resmeans,4,NR,byrow=TRUE)
#sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0("Rab_min_Intensities",ChannelName,".pdf"))
#plotresf(means[,swap],sems[,swap],paste0("Min RABx Intensity :",paste(seq(0.1,0.4,0.1), collapse = ' ')), MaxColocDisp)
#dev.off()

###Min Size effect
#resmeans=NULL
#ressems=NULL
#  for(tMinSize in seq(2,21,6)){
#  	    restemp=coloc(dtl, Int, Overlap, ColocInt, MaxSize, tMinSize,NR,0)
#	resmeans=c(resmeans,restemp$mean)
#	ressems=c(ressems,restemp$sem)
#   			   }
#means = matrix(resmeans,4,NR,byrow=TRUE)
#sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0("Min_ves_size",ChannelName,".pdf"))
#plotresf(means[,swap],sems[,swap],paste0("Min Size :",paste(seq(2,21,6), collapse = ' ')), MaxColocDisp)
#dev.off()

###Max Size effect
#resmeans=NULL
#ressems=NULL
#  for(tMaxSize in seq(100,2600,800)){
#  	    restemp=coloc(dtl, Int, Overlap, ColocInt, tMaxSize, MinSize,NR,0)
#	resmeans=c(resmeans,restemp$mean)
#	ressems=c(ressems,restemp$sem)
#   			   }
#means = matrix(resmeans,4,NR,byrow=TRUE)
#sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0("Max_ves_size",ChannelName,".pdf"))
#plotresf(means[,swap],sems[,swap],paste0("Max Size :",paste(seq(100,2600,800), collapse = ' ')), MaxColocDisp)
#dev.off()

###Overlap Effect
#resmeans=NULL
#ressems=NULL
#  for(tOverlap in seq(0.1,0.8,0.2)){
#  	    restemp=coloc(dtl, Int, tOverlap, ColocInt, MaxSize, MinSize,NR,0)
#	resmeans=c(resmeans,restemp$mean)
#	ressems=c(ressems,restemp$sem)
#   			   }
#means = matrix(resmeans,4,NR,byrow=TRUE)
#sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0("Overlap",ChannelName,".pdf"))
#plotresf(means[,swap],sems[,swap],"Overlap", MaxColocDisp)
#dev.off()




###Max Size effect on colocib
#resmeans=NULL
#ressems=NULL
#  for(tMaxSize in maxrange){
#  	    restemp=colocib(dtl, tMaxSize, MinSize,NR,0, OverlapName, order)
#	resmeans=c(resmeans,restemp$mean)
#	ressems=c(ressems,restemp$sem)
#   			   }
#means = matrix(resmeans,4,NR,byrow=TRUE)
#sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0(dir2,"Max_ves_size_effect",order,".pdf"))
#plotresf(means[,swap],sems[,swap],paste0("Max Size :",paste(maxrange, collapse = ' ')), 100*(max(means) + 2*max(sems)))
#dev.off()








###Max Size effect on colocib
#resmeans=NULL
#ressems=NULL
#  for(tMaxSize in maxrange){
#  	    restemp=colocib(dtlr, tMaxSize, MinSize,NR,0, OverlapName, order)
#	resmeans=c(resmeans,restemp$mean)
#	ressems=c(ressems,restemp$sem)
#   			   }
#means = matrix(resmeans,4,NR,byrow=TRUE)
#sems = matrix(ressems,4,NR,byrow=TRUE)
#pdf(paste0(dir2,"Max_ves_size_effect",order,".pdf"))
#plotresf(means[,swap],sems[,swap],paste0("Max Size :",paste(maxrange, collapse = ' ')), 100*(max(means) + 2*max(sems)))
#dev.off()




###########