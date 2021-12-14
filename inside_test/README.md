Данное решение принимает на входе(TCP port 8080) HTTP POST запрос с телом в JSON виде 
{
   name: "имя отправителя"
   password: "пароль"
}

Проводит авторизацию через БД, в которой заведены пользователи:
kostas, пароль 123321
test, пароль 123321

После успешной авторизации создает JWT токен:
{"alg":"HS256","typ":"JWT"}
{"name":"kostas","exp":1639467547}
{...}

и передает его в JSON'е:
{
   token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoia29zdGFzIiwiZXhwIjoxNjM5NDY3NTQ3fQ.ylHOqcr1Nj-_KZKhJZFiOJCDPZ-wwtGGwNm4yrp7Z-o"
}

Также сервер слушает запросы на добавление собщения, тело запроса:
{
   name:       "имя отправителя",
   message:    "текст сообщение"
}

И просмотр 10 последних сообщений, тело запроса:
{
   name:       "имя отправителя",
   message:    "history 10"
}

В обоих случаях должен быть предоставлен в хидере AUTHORIZATION Bearer с полученным при авторизации токеном.

Больше деталей можно узнать посмотрев класс MainTest.java


Для запуска контейнера необходимо выполнить docker run -p8080:8080 w5277c/inside_test_task
