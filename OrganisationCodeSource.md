Ce document décrit l'organisation du code présent dans le repository OpenConcerto.

# Packages principaux #

## org.jopenchart ##
Ce package contient le code de la librairie jOpenChart, librairie développée par ILM Informatique utilisée pour afficher histogrammes et diagrammes.

## org.jopendocument ##
Contient le code d'extension de la librairie jOpenDocument, librairie développée par ILM Informatique utilisée pour afficher, imprimer et convertir en PDF les documents (aux formats OpenDocument).

## org.openconcerto.erp ##
Classes qui regroupent les fonctionnalités de l'ERP.
Le sous package org.openconcerto.erp.core est subdivisé par domaine et module. (Ex: domaine: finance, module tax).
Les sous packages autorisés pour les modules sont:
  * action : actions relatives aux éléments
  * component : composants graphiques d'édition
  * element : interface de description
  * injector : migration de données entre éléments
  * io : entrées/sorties
  * model : model de données
  * report : états, rapports et documents générés
  * ui : interfaces graphiques

## org.openconcerto.ftp ##
Classes utilitaires de transfert via le protocole FTP.

## org.openconcerto.map ##
Classes relatives à la geolocalisation et l'affichage des cartes.

## org.openconcerto.openoffice ##
Classes relatives à la liaison OpenOffice et la génération de documents.

## org.openconcerto.ql ##
Classes relatives à l'impression sur imprimante QL. (Imprimantes ticket)

## org.openconcerto.sql ##
Framework d'accès aux bases de données (H2, MySQL, Derby, PostgreSQL,...). Le framework fournit un modèle riche de graphe de base de données.

## org.openconcerto.task ##
Classes relatives à la gestion de tâches

## org.openconcerto.ui ##
Classes relatives à la gestion de interfaces graphiques

## org.openconcerto.utils ##
Classes utilitaires utilisées par les frameworks et l'ERP.

## org.openconcerto.xml ##
Classes utilisées pour la persistence de données au format XML

