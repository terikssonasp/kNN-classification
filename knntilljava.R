#@author Tova Eriksson-Asp och Elin Ekman

library(class)
library(caret)
library(e1071)
options(scipen = 999, digits=2)
data(alldata)
set.seed(456)

#kNN med k-fold
indxTrain <- createDataPartition(y = alldata$target1, p = c(trainData, testData),list = FALSE) #dela upp data i tr�nings och testm�ngd
training <- alldata[indxTrain,] #positiv sida av indxTrain
testing <- alldata[-indxTrain,] #negativ sida av indxTrain

ctrl <- trainControl(method = "cv", number=kfoldValue) #bygger upp modell f�r validering av data, cv anger cross validation, number anger k-v�rde

#utf�r k-fold
knnFit <- try(train(target1~., data = training, method = "knn", trControl = ctrl, preProcess = c("center","scale"))) #best�mmer hur v�l knn analyserar datam�ngd
                                                                                # center och scale scalar data enligt z-score f�r att inte f� ett biased resultat
knnPredict<-predict(knnFit, newdata=testing) #f�rbereder genom att ta fram predicerade v�rden
kfoldSum<-try(confusionMatrix(knnPredict, testing$target1)) #matchar prediktion med targetklass i en confusionmatrix

precision<-diag(kfoldSum$table) / rowSums(kfoldSum$table) #ber�knar precision per klass utifr�n confusionmatrix (kfoldSum)