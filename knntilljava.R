#@author Tova Eriksson-Asp och Elin Ekman

library(class)
library(caret)
library(e1071)
options(scipen = 999, digits=2)
data(alldata)
set.seed(456)

#kNN med k-fold
indxTrain <- createDataPartition(y = alldata$target1, p = c(trainData, testData),list = FALSE) #dela upp data i tränings och testmängd
training <- alldata[indxTrain,] #positiv sida av indxTrain
testing <- alldata[-indxTrain,] #negativ sida av indxTrain

ctrl <- trainControl(method = "cv", number=kfoldValue) #bygger upp modell för validering av data, cv anger cross validation, number anger k-värde

#utför k-fold
knnFit <- try(train(target1~., data = training, method = "knn", trControl = ctrl, preProcess = c("center","scale"))) #bestämmer hur väl knn analyserar datamängd
                                                                                # center och scale scalar data enligt z-score för att inte få ett biased resultat
knnPredict<-predict(knnFit, newdata=testing) #förbereder genom att ta fram predicerade värden
kfoldSum<-try(confusionMatrix(knnPredict, testing$target1)) #matchar prediktion med targetklass i en confusionmatrix

precision<-diag(kfoldSum$table) / rowSums(kfoldSum$table) #beräknar precision per klass utifrån confusionmatrix (kfoldSum)