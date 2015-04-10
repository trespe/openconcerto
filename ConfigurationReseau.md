Voici un exemple de configuration réseau multiposte utilisant PostgreSQL.

Dans cet exemple, le serveur à l'adresse IP : 192.168.1.10<br>

Le fichier de configuration <b>main.properties</b> (cf les logs pour les chemins) doit contenir les lignes:<br>
<pre><code>server.ip=192.168.1.10:5432<br>
server.driver=postgresql<br>
systemRoot=OpenConcerto<br>
customer=Gestion_Default<br>
</code></pre>
