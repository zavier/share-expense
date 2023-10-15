var dataJson =
{
    "type": "page",
    "asideResizor": true,
    "asideMinWidth": 150,
    "asideMaxWidth": 400,
    "aside": [
        {
            "type": "tpl",
            "tpl": "这是侧边栏部分"
        }
    ],
    "toolbar": [
        {
            "type": "wrapper",
            "body": {
                "type": "flex",
                "justify": "flex-end",
                "items": [
                    {
                        "type": "button-group",
                        "buttons": [
                            {
                                "visibleOn": "${cookie:jwtToken == null}",
                                "type": "button",
                                "label": "注册",
                                "actionType": "dialog",
                                "dialog": {
                                    "title": "注册",
                                    "body": {
                                        "type": "form",
                                        "api": "post:/user/add",
                                        "body": [
                                            {
                                                "type": "input-text",
                                                "name": "username",
                                                "label": "昵称",
                                                "required": true
                                            },
                                            {
                                                "type": "input-email",
                                                "name": "email",
                                                "label": "邮箱",
                                                "required": true
                                            },
                                            {
                                                "type": "input-password",
                                                "name": "password",
                                                "label": "密码",
                                                "required": true
                                            }
                                        ]
                                    }
                                }
                            },
                            {
                                "visibleOn": "${cookie:jwtToken == null}",
                                "type": "button",
                                "label": "登陆",
                                "actionType": "dialog",
                                "dialog": {
                                    "title": "登陆",
                                    "body": {
                                        "type": "form",
                                        "api": "post:/user/login",
                                        "redirect": "/",
                                        "body": [
                                            {
                                                "type": "input-text",
                                                "name": "username",
                                                "label": "昵称",
                                                "required": true
                                            },
                                            {
                                                "type": "input-password",
                                                "name": "password",
                                                "label": "密码",
                                                "required": true
                                            }
                                        ]
                                    }
                                }
                            }
                        ]
                    }
                ]
            }

        }
    ],
    "body": [
        {
            "label": "创建项目",
            "type": "button",
            "actionType": "dialog",
            "level": "primary",
            "className": "m-b-sm",
            "dialog": {
                "title": "新增项目",
                "body": {
                    "type": "form",
                    "api": "post:/project/create",
                    "body": [
                        {
                            "type": "input-text",
                            "name": "projectName",
                            "label": "项目名称",
                            "required": true
                        },
                        {
                            "type": "input-text",
                            "name": "projectDesc",
                            "label": "项目描述",
                            "required": true
                        }
                    ]
                }
            }
        },
        {
            "type": "crud",
            "api": "/project/list?page=${page}&size=${perPage}",
            "syncLocation": false,
            "columns": [
                {
                    "name": "projectId",
                    "label": "项目ID"
                },
                {
                    "name": "projectName",
                    "label": "项目名称"
                },
                {
                    "name": "projectDesc",
                    "label": "项目描述"
                },
                {
                    "type": "operation",
                    "label": "操作",
                    "buttons": [
                        {
                            "label": "费用明细",
                            "type": "button",
                            "level": "link",
                            "actionType": "dialog",
                            "dialog": {
                                "title": "费用明细",
                                "body": {
                                    "type": "service",
                                    "api": "/expense/listRecord?projectId=${projectId}",
                                    "body": [
                                      {
                                        "type": "table",
                                        "title": "表格1",
                                        "source": "$rows",
                                        "columns": [
                                          {
                                            "name": "userId",
                                            "label": "用户ID"
                                          },
                                          {
                                            "name": "userName",
                                            "label": "用户姓名"
                                          },
                                          {
                                            "name": "expenseType",
                                            "label": "费用类型"
                                          },
                                          {
                                            "name": "amount",
                                            "label": "金额"
                                          },
                                          {
                                            "name": "remark",
                                            "label": "备注"
                                          },
                                        ]
                                      }
                                    ]
                                  }
                            }
                        },
                        {
                            "label": "新增费用",
                            "type": "button",
                            "level": "link",
                            "actionType": "dialog",
                            "dialog": {
                                "title": "费用信息",
                                "body": {
                                    "type": "form",
                                    "api": "post:/expense/addRecord",
                                    "body": [
                                        {
                                            "type": "input-number",
                                            "name": "projectId",
                                            "label": "项目ID",
                                            "required": true,
                                            "readOnly": true
                                        },
                                        {
                                            "type": "input-number",
                                            "name": "amount",
                                            "label": "金额",
                                            "precision": 2,
                                            "min": 0,
                                            "required": true
                                        },
                                        {
                                            "type": "input-date",
                                            "name": "date",
                                            "label": "日期",
                                            "required": true,
                                            "value": "today"
                                        },
                                        {
                                            "label": "费用类型",
                                            "type": "select",
                                            "name": "expenseType",
                                            "options": [
                                                {
                                                    "label": "饮食",
                                                    "value": "饮食"
                                                },
                                                {
                                                    "label": "娱乐",
                                                    "value": "娱乐"
                                                }
                                            ]
                                        },
                                        {
                                            "type": "input-text",
                                            "name": "reamrk",
                                            "label": "备注"
                                        }
                                    ]
                                }
                            }
                        }
                    ]
                }
            ]
        }
    ]
}