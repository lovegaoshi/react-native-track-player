global.testFunction = async (data: string) => {
  console.log(`[testFunction] input:${data}`);
  return { data: data };
};

export default () => {};
