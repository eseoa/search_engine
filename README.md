# search_engine
Поисковый движок с системой индексации сайтов работающий на jdk 17.0.1. Программа в многопоточном сайте индексирует сайты перечисленные в application.yaml. 
Индексация происходит путем прохода по каждой странице с помощью ForkJoinPool и RecursiveTask, преобразовании слов к лемме и добавлении леммы в базу вместе с выставленным ей рангом.
Поиск осуществляется путем приведения слов в искомой строке к их леммам и дальнейшего поиска этих лемм по базе данных.
Проект работает только с кодировкой UTF-8
## Список используемых технологий ##
Проект написан на java 17, в качестве базы данных была выбрана MySQL.
* ORM - hibernate 5.6.1
* spring-boot-starter-web 2.5.6
* spring-boot-starter-thymeleaf 2.5.6
* lombok 1.18.22
* Junit 4.13.2
* Jsoup 1.14.3
* Для леммитизации слов был выбран лемматизатором, который используется в поисковом движке
Apache Solr: https://github.com/akuznetsov/russianmorphology.
## Инструкция по запуску ##
Для запуска обязательно нужно задать в application.yaml (search_engine/srs/main/resources/application.yaml) сайты по которым будет происходить индексация и поиск, UserAgent и порт по которому будет запуск.

Пример:

![image](https://user-images.githubusercontent.com/46792824/172185185-6c717b92-e60a-4f65-b492-0bd3d28f0779.png)

Также обязательно настроить hibernate config (search_engine/srs/main/resources/hibernate.cfg.xml), а именно задать url, имя пользователя, пароль, диалект и максимальное количество подключений 
(максимальное количество доступных подключений должно быть не меньше количество потоков, связано это с тем что нет ограничения на потоки, при индексации сайта, программа будет использовать все доступные ресурсы). При первом запуске приложения значение <property name="hbm2ddl.auto"> должно быть create, для создания БД в дальнейшем изменить на validate

Пример:

![image](https://user-images.githubusercontent.com/46792824/172186321-c0d39636-1d37-4046-a912-68eed5214b2e.png)
  ![image](https://user-images.githubusercontent.com/46792824/172190126-6aededa5-f06c-486a-96f6-b2188ce2f4de.png)


Также для старта приложения необходимо иметь файл index.html который будет отвечать за вывод web страницы и откуда будут приходить запросы в контроллер MainPageController,
index.html должен находить в search_engine/srs/main/resources/templates
