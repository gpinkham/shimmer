/** @ngInject */
export function runMock($httpBackend: angular.IHttpBackendService) {

    var configuration = {
        shimName: 'Withings',
        settings: <ConfigurationSetting[]>[
            <ConfigurationSetting>{
                settingId: 'string.test.path',
                type: 'string',
                length: 12,
                label: 'String mock',
                description: 'A required string mock that must be 12 characters',
                required: true
            },
            <ConfigurationSetting>{
                settingId: 'boolean.test.path',
                type: 'boolean',
                label: 'Boolean mock',
                description: 'An optional boolean with a super long description that must be truncated.',
                required: false
            },
            <ConfigurationSetting>{
                settingId: 'integer.test.path',
                type: 'integer',
                min: 0,
                max: 100,
                label: 'Integer mock with a really long label',
                required: true
            },
            <ConfigurationSetting>{
                settingId: 'float.test.path',
                type: 'float',
                label: 'Float mock',
                required: false
            }
        ],
        values: [
          { settingId:'string.test.path', value:'dfhg29h92020' },
          { settingId:'boolean.test.path', value: 'false' },
          { settingId:'integer.test.path', value: '3' },
          { settingId:'float.test.path', value: '1.01' },
        ]
    };

    var schemas = <SchemaList>{
        shimName: 'Withings',
        schemas: <Schema[]>[
            <Schema>{
                    'namespace': 'granola',
                    name: 'schema-name',
                    version: '1.x',
                    measures: [
                      'distance',
                      'pies eaten'
                    ]
                },
            <Schema>{
                    'namespace': 'omh',
                    name: 'schema-name',
                    version: '1.2',
                    measures: [
                        'step count',
                        'meatball index'
                    ]
                },
            <Schema>{
                    'namespace': 'omh',
                    name: 'schema-name',
                    version: '1.1',
                    measures: [
                        'step count',
                        'melbatoast index'
                    ]
                },
            <Schema>{
                    'namespace': 'omh',
                    name: 'schema-name',
                    version: '1.2',
                    measures: [
                        'step count',
                        'meatball index'
                    ]
                },
            <Schema>{
                    'namespace': 'omh',
                    name: 'schema-with-name',
                    version: '1.11',
                    measures: [
                        'step count',
                        'melbatoast index'
                    ]
                },
            <Schema>{
                    'namespace': 'omh',
                    name: 'schema-schema-name',
                    version: '1.2',
                    measures: [
                        'step count',
                        'meatball index'
                    ]
                },
                <Schema>{
                    'namespace': 'omh',
                    name: 'schema-other-name',
                    version: '1.1',
                    measures: [
                        'step count',
                        'melbatoast index'
                    ]
                },
                <Schema>{
                    'namespace': 'omh',
                    name: 'schema-name',
                    version: '1.2',
                    measures: [
                        'step count',
                        'meatball index'
                    ]
                },
                <Schema>{
                    'namespace': 'omh',
                    name: 'schema-name-also',
                    version: '1.1',
                    measures: [
                        'step count',
                        'melbatoast index'
                    ]
                },
                <Schema>{
                    'namespace': 'omh',
                    name: 'schemas-name',
                    version: '1.2',
                    measures: [
                        'step count',
                        'meatball index'
                    ]
                },
                <Schema>{
                    'namespace': 'granola',
                    name: 'schema-name',
                    version: '1.1',
                    measures: [
                        'step count',
                        'melbatoast index'
                    ]
                }
              ]
      };

    // returns the current list of configurations
    $httpBackend.whenGET(/\/api\/configuration/).respond([configuration]);
    // returns the current list of schemas
    $httpBackend.whenGET(/\/api\/schemas/).respond([schemas]);

    // adds a new phone to the phones array
    $httpBackend.whenPOST(/^\/api\/.+\/configuration/).respond(function(method: any, url: string, data: string) {
    var newConfiguration = angular.fromJson(data);
    configuration = newConfiguration;
    return [200, newConfiguration, {}];
  });

  $httpBackend.whenGET(/^\/api\/[.+\/]?authorizations/).passThrough();
  $httpBackend.whenGET(/app\//).passThrough();

}


