var dataJson =
{
    "type": "page",
    "asideResizor": true,
    "asideMinWidth": 150,
    "asideMaxWidth": 400,
    "aside": [
        {
            "type": "tpl",
            "tpl": ""
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
                                        "api": "post:/expense/user/add",
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
                                        "api": "post:/expense/user/login",
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
                    "api": "post:/expense/project/create",
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
                            "required": false
                        }
                    ]
                }
            }
        },
        {
            "type": "crud",
            "api": "/expense/project/list?page=${page}&size=${perPage}",
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
                            "label": "添加成员",
                            "type": "button",
                            "level": "link",
                            "actionType": "dialog",
                            "dialog": {
                                "title": "添加成员",
                                "body": {
                                    "type": "form",
                                    "api": "post:/expense/project/addMember",
                                    "body": [
                                        {
                                            "type": "input-number",
                                            "name": "projectId",
                                            "label": "项目ID",
                                            "required": true,
                                            "readOnly": true,
                                            "visible": false
                                        },
                                        {
                                            "label": "成员",
                                            "type": "select",
                                            "name": "userId",
                                            "source": "/user/list?page=1&size=1000",
                                            "labelField": "userName",
                                            "valueField": "userId",
                                            "required": true,
                                        },
                                        {
                                            "type": "input-number",
                                            "name": "weight",
                                            "label": "项目分摊权重",
                                            "required": true,
                                            "displayMode": "enhance",
                                            "min": 1,
                                            "precision": 0,
                                            "value": 1

                                        },
                                    ]
                                }
                            }
                        },
                        {
                            "label": "项目成员",
                            "type": "button",
                            "level": "link",
                            "actionType": "dialog",
                            "dialog": {
                                "title": "项目成员",
                                "body": {
                                    "type": "service",
                                    "api": "/expense/project/pageMember?projectId=${projectId}",
                                    "body": {
                                        "type": "table",
                                        "source": "$rows",
                                        "columns": [
                                            {
                                                "name": "userName",
                                                "label": "用户姓名"
                                            },
                                            {
                                                "name": "weight",
                                                "label": "分摊权重(人份)"
                                            }
                                        ]

                                    }
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
                                    "api": "post:/expense/project/addRecord",
                                    "body": [
                                        {
                                            "type": "input-number",
                                            "name": "projectId",
                                            "label": "项目ID",
                                            "required": true,
                                            "readOnly": true
                                        },
                                        {
                                            "label": "花费人",
                                            "type": "select",
                                            "name": "userId",
                                            "source": "/expense/project/listMember?projectId=${projectId}",
                                            "labelField": "userName",
                                            "valueField": "userId",
                                            "required": true,
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
                                            "required": true,
                                            "creatable": true,
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
                        },
                        {
                            "label": "费用明细",
                            "type": "button",
                            "level": "link",
                            "actionType": "dialog",
                            "dialog": {
                                "title": "费用明细",
                                "body": {
                                    "type": "service",
                                    "api": "/expense/project/listRecord?projectId=${projectId}",
                                    "body": [
                                        {
                                            "type": "table",
                                            "title": "明细信息",
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
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                        },
                        {
                            "label": "分摊明细",
                            "type": "button",
                            "level": "link",
                            "actionType": "dialog",
                            "dialog": {
                                "title": "分摊明细",
                                "body": {
                                    "type": "service",
                                    "api": "/expense/project/sharing?projectId=${projectId}",
                                    "body": [
                                        {
                                            "type": "table",
                                            "title": "明细信息",
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
                                                    "name": "needPaid",
                                                    "label": "应付金额"
                                                },
                                                {
                                                    "name": "needReceive",
                                                    "label": "应收金额"
                                                },
                                                {
                                                    "name": "shareAmount",
                                                    "label": "需分摊金额"
                                                },
                                                {
                                                    "name": "paidAmount",
                                                    "label": "已花费金额"
                                                }
                                            ]
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