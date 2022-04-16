#!/bin/bash

MY_USER="test"
ADMIN_USER="admin"
ARCHIVA_URL="http://localhost:8080"

while [ ! -z "$1" ]; do
   case $1 in
     -u)
       shift
       MY_USER_PWD="$1"
       ;;
     -a)
       shift
       ADMIN_PWD="$1"
       ;;
     *)
       ;;
   esac
   shift
done

if [ -z "${MY_USER_PWD}" ]; then
  read -s -p "Enter Password of user ${MY_USER}: " MY_USER_PWD
  echo " "
fi
if [ -z "${ADMIN_PWD}" ]; then
  read -s -p "Enter Password of user ${ADMIN_USER}: " ADMIN_PWD
  echo " "
fi

echo "Login with user ${MY_USER}"
OUTPUT=$(curl -s -w '\nhttp_code=%{response_code}\n' --cookie-jar cookies.txt -H "Content-Type: application/xml" -H "Accept: application/json" -H "Origin: ${ARCHIVA_URL}" -d "<loginRequest><username>${MY_USER}</username><password>${MY_USER_PWD}</password></loginRequest>" "${ARCHIVA_URL}/restServices/redbackServices/loginService/logIn")

CODE=$(echo $OUTPUT |sed -n -e 's/.*http_code=\(.*\)/\1/gp')
TOKEN=$(echo $OUTPUT |sed -n -e 's/.*"validationToken":"\([^"]\+\)".*/\1/gp')

if [ "$CODE" != "200" ]; then
  echo "Login with user ${MY_USER} failed. HTTP Response: $CODE"
  echo "$OUTPUT"
  exit 1
fi

NEW_MY_USER_PWD="$(cat /dev/urandom | tr -dc 'a-z0-9' | fold -w 10 | head -n 1)"
echo "Updating user password with new value: ${NEW_MY_USER_PWD}"
OUTPUT=$(curl -s --cookie cookies.txt -w '\nhttp_code=%{response_code}\n' -d "<user><username>${MY_USER}</username><password>${NEW_MY_USER_PWD}</password><validated>true</validated></user>" -H "X-XSRF-TOKEN: ${TOKEN}" -H "Content-Type: application/xml" -H "Origin: ${ARCHIVA_URL}" -H "Accept: application/json"  "${ARCHIVA_URL}/restServices/redbackServices/userService/updateUser")

CODE=$(echo $OUTPUT |sed -n -e 's/.*http_code=\(.*\)/\1/gp')
if [ "${CODE}" != "200" ]; then
  echo "Could not update user password"
  echo "HTTP Response: $CODE"
  echo "$OUTPUT"
  exit 1
fi

NEW_ADMIN_PWD="$(cat /dev/urandom | tr -dc 'a-z0-9' | fold -w 10 | head -n 1)"
echo "Trying to update admin password with new value: ${NEW_ADMIN_PWD}"

OUTPUT=$(curl -s --cookie cookies.txt -w '\nhttp_code=%{response_code}\n' -d "<user><username>${ADMIN_USER}</username><password>${NEW_ADMIN_PWD}</password><validated>true</validated></user>" -H "X-XSRF-TOKEN: ${TOKEN}" -H "Content-Type: application/xml" -H "Origin: ${ARCHIVA_URL}" -H "Accept: application/json"  "${ARCHIVA_URL}/restServices/redbackServices/userService/updateUser")

echo "$OUTPUT"

CODE=$(echo $OUTPUT |sed -n -e 's/.*http_code=\(.*\)/\1/gp')
if [ "${CODE}" == "200" ]; then
  echo "Could update admin password as normal user! This should not happen."
  exit 1
elif [ "${CODE}" == "403" ]; then
  echo "This is fine. Could not update admin password as normal user."
else
  echo "Unexpected response while updating admin password"
  echo "$OUTPUT"
  exit 1
fi

NEW_MY_USER_PWD="$(cat /dev/urandom | tr -dc 'a-z0-9' | fold -w 10 | head -n 1)"
echo "Setting password for user ${MY_USER} by using admin account to new value: ${NEW_MY_USER_PWD}"

rm -f cookies.txt
OUTPUT=$(curl -s -w '\nhttp_code=%{response_code}\n' --cookie-jar cookies.txt -H "Content-Type: application/xml" -H "Accept: application/json" -H "Origin: ${ARCHIVA_URL}" -d "<loginRequest><username>${ADMIN_USER}</username><password>${ADMIN_PWD}</password></loginRequest>" "${ARCHIVA_URL}/restServices/redbackServices/loginService/logIn")
TOKEN=$(echo $OUTPUT |sed -n -e 's/.*"validationToken":"\([^"]\+\)".*/\1/gp')

OUTPUT=$(curl -s --cookie cookies.txt -w '\nhttp_code=%{response_code}\n' -d "<user><username>${MY_USER}</username><password>${NEW_MY_USER_PWD}</password><validated>true</validated></user>" -H "X-XSRF-TOKEN: ${TOKEN}" -H "Content-Type: application/xml" -H "Origin: ${ARCHIVA_URL}" -H "Accept: application/json"  "${ARCHIVA_URL}/restServices/redbackServices/userService/updateUser")

CODE=$(echo $OUTPUT |sed -n -e 's/.*http_code=\(.*\)/\1/gp')
if [ "$CODE" != "200" ]; then
  echo "Error during user password update"
  echo "$OUTPUT"
  exit 1
fi

echo "Current password for user ${MY_USER}: ${NEW_MY_USER_PWD}"
