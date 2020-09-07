# Copyright 2017-2020 EPAM Systems, Inc. (https://www.epam.com/)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import json
import os
import requests
import sys
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

API_GET_ALL_USERS = 'users'
API_CREATE_USER = 'user'
API_BLOCK_USER = 'user/{user_id}/block'
API_SET_GROUP_BLOCK_STATUS = 'group/{group_name}/block'
API_GET_ALL_BLOCK_STATUSES = 'groups/block'

API_GET_ALL_ROLES = 'role/loadAll'
API_CREATE_ROLE = 'role/create'
API_ASSIGN_ROLE_TO_USER = 'role/{role_id}/assign'

API_METADATA_LOAD = 'metadata/load'
API_METADATA_UPDATE = 'metadata/update'

metadata_keys_to_ignore = os.getenv('CP_SYNC_USERS_METADATA_SKIP_KEYS', '').split(',')


class API(object):
    def __init__(self, api_path, access_key):
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        self.api = api_path
        self.__headers__ = {'Content-Type': 'application/json',
                            'Authorization': 'Bearer {}'.format(access_key)}

    def get_url_for_method(self, method):
        return '{}/{}'.format(self.api.strip('/'), method)

    def get_headers(self):
        return self.__headers__

    def call(self, method, data=None, params=None, http_method=None, error_message=None):
        url = '{}/{}'.format(self.api.strip('/'), method)
        if not http_method:
            if data:
                response = requests.post(url, data, headers=self.__headers__, verify=False)
            else:
                response = requests.get(url, headers=self.__headers__, verify=False)
        else:
            if http_method.lower() == 'get':
                response = requests.get(url, headers=self.__headers__, params=params, verify=False)
            elif http_method.lower() == 'post':
                response = requests.post(url, data, headers=self.__headers__, params=params, verify=False)
            elif http_method.lower() == 'delete':
                if data:
                    response = requests.delete(url, data=data, headers=self.__headers__, verify=False)
                else:
                    response = requests.delete(url, headers=self.__headers__, verify=False)
            else:
                if data:
                    response = requests.post(url, data, headers=self.__headers__, verify=False)
                else:
                    response = requests.get(url, headers=self.__headers__, verify=False)
        response_data = json.loads(response.text)
        message_text = error_message if error_message else 'Failed to fetch data from server'
        if 'status' not in response_data:
            raise RuntimeError('{}. Server responded with status: {}.'
                               .format(message_text, str(response_data.status_code)))
        if response_data['status'] != 'OK':
            raise RuntimeError('{}. Server responded with message: {}'.format(message_text, response_data['message']))
        else:
            return response_data

    def update_group_blocking_status(self, group_name, status=True):
        data = self.call(API_SET_GROUP_BLOCK_STATUS.format(group_name=group_name),
                         params={'blockStatus': str(status).lower()})
        return data['payload']

    def load_all_roles(self):
        data = self.call(API_GET_ALL_ROLES, http_method='GET')
        roles = data['payload']
        if not roles:
            roles = []
        return roles

    def create_role(self, role_name, is_user_default):
        data = self.call(API_CREATE_ROLE, params={'roleName': role_name, 'userDefault': is_user_default},
                         http_method='POST')
        return data['payload']

    def load_all_users(self):
        data = self.call(API_GET_ALL_USERS, http_method='GET')
        users = data['payload']
        if not users:
            users = []
        return users

    def create_user(self, name, roles_ids):
        data = self.call(API_CREATE_USER, data=json.dumps({'userName': name, 'roleIds': roles_ids}), http_method='POST')
        return data['payload']

    def get_available_groups_blocking(self):
        data = self.call(API_GET_ALL_BLOCK_STATUSES, http_method='GET')
        blocking_statuses = data['payload']
        if not blocking_statuses:
            blocking_statuses = []
        return {status['groupName']: status['blocked'] for status in blocking_statuses}

    def set_user_blocking(self, user_id, status):
        self.call(API_BLOCK_USER.format(user_id), params={'blockStatus': status}, http_method='GET')

    def assign_users_to_roles(self, role_id, user_ids):
        self.call(API_ASSIGN_ROLE_TO_USER.format(role_id=role_id), params={'userIds': user_ids})

    def load_entities_metadata(self, entities_ids, entity_class):
        data = []
        for entity_id in entities_ids:
            data.append({'entityId': entity_id, 'entityClass': entity_class})
        response = self.call(API_METADATA_LOAD, data=json.dumps(data), http_method='POST')
        if response['payload']:
            return response['payload']
        else:
            return []

    def upload_metadata(self, metadata_entity):
        self.call(API_METADATA_UPDATE, data=json.dumps(metadata_entity), http_method='POST')


def sync_roles(api_source, api_target):
    roles_a = api_source.load_all_roles()
    roles_b = api_target.load_all_roles()
    roles_dict = {role['name']: role for role in roles_b}
    for src_role in roles_a:
        role_name = src_role['name']
        if role_name not in roles_dict:
            print('Role [{}] doesn\'t exist in the target deployment, try creating it'.format(role_name))
            new_role = api_target.create_role(role_name, src_role['userDefault'])
            if not new_role:
                print('  Error creating role in the target deployment, it will be skipped!'.format(role_name))
                continue
            else:
                print('  Role created in the target deployment successfully!'.format(role_name))
                new_role['blocked'] = False
                roles_dict[new_role['name']] = new_role
        else:
            print('Role [{}] exists in the target deployment already'.format(role_name))
        required_blocking_status = src_role['blocked']
        current_target_blocking_status = roles_dict[role_name]['blocked']
        if required_blocking_status != current_target_blocking_status:
            print('Update blocking status for role [{}] in the target deployment to `{}`'
                  .format(role_name, required_blocking_status))
            api_target.update_group_blocking_status(role_name, required_blocking_status)
    name_to_id_mapping = {role_name: role_obj['id'] for role_name, role_obj in roles_dict.items()}
    roles_ids_mapping = {role['id']: roles_dict[role['name']]['id'] for role in roles_a}
    return name_to_id_mapping, roles_ids_mapping


def sync_groups_statuses(api_source, api_target):
    groups_blocking_a = api_source.get_available_groups_blocking()
    groups_blocking_b = api_target.get_available_groups_blocking()
    for group_name, blocking_status in groups_blocking_a.items():
        if group_name not in groups_blocking_b:
            if blocking_status:
                print('Missed blocking status for a group [{}]'.format(group_name))
                api_target.update_group_blocking_status(group_name, blocking_status)
        else:
            current_target_blocking_status = groups_blocking_b[group_name]
            if blocking_status != current_target_blocking_status:
                print('Update blocking status for group [{}] in the target deployment to `{}`'
                      .format(group_name, blocking_status))
            api_target.update_group_blocking_status(group_name, blocking_status)


def create_new_user_in_target(api_target, roles_mapping, user):
    roles_ids = []
    for role in user['roles']:
        roles_ids.append(roles_mapping.get(role['name']))
    return api_target.create_user(name=user['userName'], roles_ids=roles_ids)


def sync_users(api_source, api_target, roles_mapping):
    user_ids_mapping = {}
    users_a = api_source.load_all_users()
    users_b = api_target.load_all_users()
    target_users_dict = {user['userName']: user for user in users_b}
    roles_to_assign = {}
    for user in users_a:
        username = user['userName']
        if username in target_users_dict:
            print('User [{}] exists in the target deployment already'.format(username))
            existing_user = target_users_dict[username]
            source_user_roles = [role['name'] for role in user['roles']]
            existing_user_roles = [role['name'] for role in existing_user['roles']]
            for role in source_user_roles:
                if role not in existing_user_roles:
                    if role not in roles_to_assign:
                        roles_to_assign[role] = [existing_user['id']]
                    else:
                        roles_to_assign[role].append(existing_user['id'])
        else:
            print('User [{}] doesn\'t exist in the target deployment, try creating it'.format(username))
            existing_user = create_new_user_in_target(api_target, roles_mapping, user)
            existing_user['blocked'] = False
            print('  User created in the target deployment successfully!'.format(username))
        user_ids_mapping[user['id']] = existing_user['id']
        if user['blocked'] != existing_user['blocked']:
            print('Update blocking status for user [{}] in the target deployment to `{}`'
                  .format(username, user['blocked']))
            api_target.set_user_blocking(existing_user['id'], user['blocked'])
    for role_name, users_ids in roles_to_assign.items():
        print('Syncing {} users for [{}] role'.format(len(users_ids), role_name))
        api_target.assign_users_to_roles(roles_mapping[role_name], users_ids)
    return user_ids_mapping


def sync_metadata_for_entity_class(api_source, api_target, ids_mapping, entity_class):
    source_metadata_entities = api_source.load_entities_metadata(ids_mapping.keys(), entity_class)
    for metadata_entity in source_metadata_entities:
        metadata_entity['entity']['entityId'] = ids_mapping[metadata_entity['entity']['entityId']]
        if 'data' in metadata_entity and metadata_entity['data']:
            metadata_entity['data'] = {key: value
                                       for key, value in metadata_entity['data'].items()
                                       if key not in metadata_keys_to_ignore}
            if metadata_entity['data']:
                api_target.upload_metadata(metadata_entity)


def sync_users_routine(api_url_a, api_token_a, api_url_b, api_token_b):
    api_source = API(api_url_a, api_token_a)
    api_target = API(api_url_b, api_token_b)
    print('\n===Start roles sync===')
    roles_mapping, roles_ids_mapping = sync_roles(api_source, api_target)
    print('===Roles sync is finished===')

    print('\n===Start groups\' blocking statuses sync===')
    sync_groups_statuses(api_source, api_target)
    print('===Groups\' blocking statuses sync is finished===')

    print('\n===Start users sync===')
    user_ids_mapping = sync_users(api_source, api_target, roles_mapping)
    print('===Users sync is finished===')

    print('\n===Start metadata sync===')
    print('  Syncing users\' metadata')
    sync_metadata_for_entity_class(api_source, api_target, ids_mapping=user_ids_mapping, entity_class='PIPELINE_USER')
    print('  Syncing roles\' metadata')
    sync_metadata_for_entity_class(api_source, api_target, ids_mapping=roles_ids_mapping, entity_class='ROLE')
    print('===Metadata sync is finished===')


if __name__ == '__main__':
    if len(sys.argv[1:]) != 4:
        raise RuntimeError('Invalid count of an arguments for sync process!')
    else:
        source_api_url = sys.argv[1]
        source_api_token = sys.argv[2]
        target_api_url = sys.argv[3]
        target_api_token = sys.argv[4]
        sync_users_routine(source_api_url, source_api_token, target_api_url, target_api_token)
