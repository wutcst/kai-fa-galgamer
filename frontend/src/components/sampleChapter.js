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
      event: { type: 'story', text: '新的循环开始了。' },
    },
  ],
}

export function sampleChapterText() {
  return JSON.stringify(SAMPLE, null, 2)
}
