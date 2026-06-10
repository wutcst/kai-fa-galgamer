const SAMPLE = {
  chapterId: 'my_custom_chapter',
  title: '自定义章节',
  author: 'Player',
  startRoomId: 'start',
  rooms: [
    {
      roomId: 'start',
      name: '起点',
      description: '你写下自己的第一间房。',
      exits: {},
      dialogue: {
        dialogueGroupId: 'dial_custom_intro',
        startNodeId: 'node_01',
        nodes: {
          node_01: {
            type: 'SPEECH',
            speakerSide: 'RIGHT',
            speakerName: '陌生旅伴',
            text: '这里是你亲手写下的第一条命运分支。',
            leftCharacter: { id: 'player', expression: 'default' },
            rightCharacter: { id: 'scholar', expression: 'default' },
            nextNodeId: 'node_choice',
          },
          node_choice: {
            type: 'CHOICE',
            choices: [
              {
                choiceId: 'accept',
                text: '继续写下去。',
                conditions: [],
                effects: [{ type: 'SET_FLAG', flagKey: 'custom_intro_accepted', booleanValue: true }],
                nextNodeId: 'node_exit',
              },
            ],
          },
          node_exit: {
            type: 'EVENT_TRIGGER',
            eventType: 'SET_FLAG',
            eventPayload: 'custom-intro',
            dialogueLog: '新的章节开始流动。',
            nextNodeId: 'EXIT',
          },
        },
      },
      event: { type: 'story', text: '新的循环开始了。' },
    },
  ],
}

export function sampleChapterText() {
  return JSON.stringify(SAMPLE, null, 2)
}
